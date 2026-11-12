using System.Net;
using System.Net.Sockets;
using System.Net.WebSockets;
using System.Security.Cryptography;
using System.Text;
using System.Text.Json;

var pin = RandomNumberGenerator.GetInt32(100000, 999999).ToString();
Console.Title = "DECKRA Bridge";
Console.WriteLine("DECKRA Bridge iniciado. PIN de pareamento: " + pin);
Console.WriteLine("Celular e PC devem estar na mesma rede Wi-Fi.");
using var udp = new UdpClient(37891);
_ = Task.Run(async () => {
    while (true) {
        var packet = await udp.ReceiveAsync();
        if (Encoding.UTF8.GetString(packet.Buffer) == "DECKRA_DISCOVER") {
            var reply = Encoding.UTF8.GetBytes("DECKRA|" + Dns.GetHostName() + "|37892");
            await udp.SendAsync(reply, packet.RemoteEndPoint);
        }
    }
});
using var listener = new HttpListener();
listener.Prefixes.Add("http://+:37892/");
try { listener.Start(); }
catch (HttpListenerException ex) { Console.WriteLine("Não foi possível abrir a porta 37892. Libere-a no Firewall do Windows para rede privada: " + ex.Message); return; }
Console.WriteLine("Aguardando conexão do aplicativo Android...");
while (true) {
    var context = await listener.GetContextAsync();
    if (!context.Request.IsWebSocketRequest) { context.Response.StatusCode = 400; context.Response.Close(); continue; }
    _ = Handle(context, pin);
}

static async Task Handle(HttpListenerContext context, string expectedPin) {
    WebSocket? ws = null;
    try {
        ws = (await context.AcceptWebSocketAsync(null)).WebSocket;
        var buffer = new byte[4096]; var paired = false;
        while (ws.State == WebSocketState.Open) {
            var result = await ws.ReceiveAsync(new ArraySegment<byte>(buffer), CancellationToken.None);
            if (result.MessageType == WebSocketMessageType.Close) break;
            using var doc = JsonDocument.Parse(buffer.AsMemory(0, result.Count));
            var root = doc.RootElement;
            var type = root.TryGetProperty("type", out var typeValue) ? typeValue.GetString() : null;
            if (type == "pair") {
                paired = root.TryGetProperty("pin", out var pinValue) && pinValue.GetString() == expectedPin;
                await Reply(ws, new { paired, message = paired ? "Dispositivo pareado." : "PIN incorreto." });
                if (!paired) break;
            } else if (type == "command" && paired) {
                var command = root.TryGetProperty("command", out var commandValue) ? commandValue.GetString() ?? "" : "";
                var ok = command switch {
                    "volume_up" => Native.keybd_event(0xAF, 0, 0, UIntPtr.Zero) == 0,
                    "volume_down" => Native.keybd_event(0xAE, 0, 0, UIntPtr.Zero) == 0,
                    "volume_mute" => Native.keybd_event(0xAD, 0, 0, UIntPtr.Zero) == 0,
                    "media_play_pause" => Native.keybd_event(0xB3, 0, 0, UIntPtr.Zero) == 0,
                    "media_next" => Native.keybd_event(0xB0, 0, 0, UIntPtr.Zero) == 0,
                    "media_previous" => Native.keybd_event(0xB1, 0, 0, UIntPtr.Zero) == 0,
                    _ => false
                };
                await Reply(ws, new { ok, command, message = ok ? "Comando enviado ao Windows." : "Comando não permitido." });
                if (ok) {
                    var releaseKey = command switch { "volume_up"=>0xAF, "volume_down"=>0xAE, "volume_mute"=>0xAD, "media_play_pause"=>0xB3, "media_next"=>0xB0, "media_previous"=>0xB1, _=>0 };
                    Native.keybd_event((byte)releaseKey, 0, 2, UIntPtr.Zero);
                }
            }
        }
    } catch (Exception ex) { Console.WriteLine("Conexão encerrada: " + ex.Message); }
    finally { if (ws != null) ws.Dispose(); }
}
static async Task Reply(WebSocket ws, object value) {
    var bytes = Encoding.UTF8.GetBytes(JsonSerializer.Serialize(value));
    await ws.SendAsync(new ArraySegment<byte>(bytes), WebSocketMessageType.Text, true, CancellationToken.None);
}
static class Native {
    [System.Runtime.InteropServices.DllImport("user32.dll", SetLastError = true)]
    internal static extern int keybd_event(byte bVk, byte bScan, uint dwFlags, UIntPtr dwExtraInfo);
}
