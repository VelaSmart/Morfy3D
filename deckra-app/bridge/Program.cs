using System.Net;
using System.Net.Sockets;
using System.Net.WebSockets;
using System.Security.Cryptography;
using System.Text;
using System.Text.Json;
using NAudio.CoreAudioApi;

var pin = RandomNumberGenerator.GetInt32(100000, 1000000).ToString();
Console.WriteLine("DECKRA_PIN=" + pin);
Console.Out.Flush();

using var udp = new UdpClient(new IPEndPoint(IPAddress.Any, 37891));
using var stop = new CancellationTokenSource();
_ = Task.Run(async () =>
{
    while (!stop.IsCancellationRequested)
    {
        try
        {
            var packet = await udp.ReceiveAsync(stop.Token);
            var message = Encoding.UTF8.GetString(packet.Buffer).Trim();
            if (message is "DECKRA_DISCOVER" or "DECKRA_DISCOVER_V2")
            {
                var bytes = Encoding.UTF8.GetBytes(JsonSerializer.Serialize(new {
                    type = "deckra_bridge", port = 37892, protocol = 2
                }));
                await udp.SendAsync(bytes, packet.RemoteEndPoint, stop.Token);
            }
        }
        catch (OperationCanceledException) { break; }
        catch { }
    }
});

using var listener = new HttpListener();
listener.Prefixes.Add("http://+:37892/");
listener.Start();
while (listener.IsListening)
{
    HttpListenerContext context;
    try { context = await listener.GetContextAsync(); }
    catch (HttpListenerException) { break; }
    if (!context.Request.IsWebSocketRequest)
    {
        context.Response.StatusCode = 400;
        context.Response.Close();
        continue;
    }
    _ = Handle(context, pin);
}

static async Task Handle(HttpListenerContext context, string expectedPin)
{
    WebSocket? ws = null;
    var paired = false;
    try
    {
        ws = (await context.AcceptWebSocketAsync(null)).WebSocket;
        var buffer = new byte[8192];
        while (ws.State == WebSocketState.Open)
        {
            using var ms = new MemoryStream();
            WebSocketReceiveResult result;
            do {
                result = await ws.ReceiveAsync(new ArraySegment<byte>(buffer), CancellationToken.None);
                if (result.MessageType == WebSocketMessageType.Close) return;
                ms.Write(buffer, 0, result.Count);
                if (ms.Length > 8192) throw new InvalidDataException("Mensagem excede o limite.");
            } while (!result.EndOfMessage);
            using var doc = JsonDocument.Parse(ms.ToArray());
            var root = doc.RootElement;
            var type = GetString(root, "type");
            if (type == "pair")
            {
                paired = GetString(root, "pin") == expectedPin;
                await Reply(ws, new { type="pair_result", paired, message=paired ? "Dispositivo pareado." : "PIN incorreto." });
                if (!paired) return;
                await SendState(ws);
            }
            else if (type == "command" && paired)
            {
                var command = GetString(root, "command");
                try {
                    object payload = Execute(command, root);
                    await Reply(ws, new { type="result", ok=true, command, data=payload });
                } catch (Exception ex) {
                    await Reply(ws, new { type="result", ok=false, command, message=ex.Message });
                }
            }
        }
    }
    catch { }
    finally { ws?.Dispose(); }
}

static object Execute(string command, JsonElement root)
{
    using var enumerator = new MMDeviceEnumerator();
    switch (command)
    {
        case "get_state": return GetState(enumerator);
        case "volume_up": SendMediaKey(0xAF); return new { };
        case "volume_down": SendMediaKey(0xAE); return new { };
        case "volume_mute": SendMediaKey(0xAD); return new { };
        case "media_play_pause": SendMediaKey(0xB3); return new { };
        case "media_next": SendMediaKey(0xB0); return new { };
        case "media_previous": SendMediaKey(0xB1); return new { };
        case "set_master_volume":
            enumerator.GetDefaultAudioEndpoint(DataFlow.Render, Role.Multimedia).AudioEndpointVolume.MasterVolumeLevelScalar = GetFloat(root, "value");
            return GetState(enumerator);
        case "set_mic_volume":
            enumerator.GetDefaultAudioEndpoint(DataFlow.Capture, Role.Communications).AudioEndpointVolume.MasterVolumeLevelScalar = GetFloat(root, "value");
            return GetState(enumerator);
        case "set_mic_mute":
            enumerator.GetDefaultAudioEndpoint(DataFlow.Capture, Role.Communications).AudioEndpointVolume.Mute = GetBool(root, "value");
            return GetState(enumerator);
        case "set_app_volume":
        case "set_app_mute":
            var processId = GetInt(root, "processId");
            var sessions = enumerator.GetDefaultAudioEndpoint(DataFlow.Render, Role.Multimedia).AudioSessionManager.Sessions;
            for (var i = 0; i < sessions.Count; i++)
            {
                var session = sessions[i];
                if (session.GetProcessID != processId) continue;
                if (command == "set_app_volume") session.SimpleAudioVolume.Volume = GetFloat(root, "value");
                else session.SimpleAudioVolume.Mute = GetBool(root, "value");
                return GetState(enumerator);
            }
            throw new InvalidOperationException("Sessão de áudio do aplicativo não encontrada.");
        case "set_equalizer":
            throw new NotSupportedException("Equalizador global requer driver APO ou dispositivo virtual.");
        default: throw new InvalidOperationException("Comando não permitido.");
    }
}

static object GetState(MMDeviceEnumerator e)
{
    var output = e.GetDefaultAudioEndpoint(DataFlow.Render, Role.Multimedia);
    var input = e.GetDefaultAudioEndpoint(DataFlow.Capture, Role.Communications);
    var sessions = output.AudioSessionManager.Sessions;
    var apps = new List<object>();
    for (var i=0; i<sessions.Count; i++)
    {
        var s=sessions[i];
        try {
            var id=s.GetProcessID;
            if (id == 0) continue;
            var process=System.Diagnostics.Process.GetProcessById((int)id);
            apps.Add(new { processId=id, name=process.ProcessName, volume=s.SimpleAudioVolume.Volume, mute=s.SimpleAudioVolume.Mute, level=s.AudioMeterInformation.MasterPeakValue });
        } catch { }
    }
    return new {
        pcName = Environment.MachineName,
        output = new { name=output.FriendlyName, volume=output.AudioEndpointVolume.MasterVolumeLevelScalar, mute=output.AudioEndpointVolume.Mute },
        microphone = new { name=input.FriendlyName, volume=input.AudioEndpointVolume.MasterVolumeLevelScalar, mute=input.AudioEndpointVolume.Mute },
        applications = apps,
        equalizer = new { available=false, reason="Requer driver APO ou dispositivo virtual com processamento de áudio." }
    };
}

static async Task SendState(WebSocket ws)
{
    try { await Reply(ws, new { type="state", data=GetState(new MMDeviceEnumerator()) }); }
    catch (Exception ex) { await Reply(ws, new { type="error", message="Falha ao consultar áudio: "+ex.Message }); }
}
static string GetString(JsonElement e,string key) => e.TryGetProperty(key,out var v) ? v.GetString() ?? "" : "";
static float GetFloat(JsonElement e,string key) { if(!e.TryGetProperty(key,out var v) || !v.TryGetSingle(out var n) || !float.IsFinite(n)) throw new ArgumentException("Valor inválido."); return Math.Clamp(n,0f,1f); }
static int GetInt(JsonElement e,string key) => e.TryGetProperty(key,out var v) && v.TryGetInt32(out var n) ? n : throw new ArgumentException("Identificador inválido.");
static bool GetBool(JsonElement e,string key) => e.TryGetProperty(key,out var v) && v.ValueKind is JsonValueKind.True or JsonValueKind.False ? v.GetBoolean() : throw new ArgumentException("Booleano inválido.");
static void SendMediaKey(byte key) { Native.keybd_event(key,0,0,UIntPtr.Zero); Native.keybd_event(key,0,2,UIntPtr.Zero); }
static async Task Reply(WebSocket ws, object value) { var bytes=Encoding.UTF8.GetBytes(JsonSerializer.Serialize(value)); await ws.SendAsync(new ArraySegment<byte>(bytes),WebSocketMessageType.Text,true,CancellationToken.None); }
static class Native { [System.Runtime.InteropServices.DllImport("user32.dll",SetLastError=true)] internal static extern void keybd_event(byte bVk,byte bScan,uint dwFlags,UIntPtr dwExtraInfo); }
