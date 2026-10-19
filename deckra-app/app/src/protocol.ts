export type DeviceMessage =
  | { type: "hello"; protocol: number; device: string; firmware: string; capabilities: string[] }
  | { type: "control"; control: string; kind: "potentiometer"; value: number; min: number; max: number }
  | { type: "control"; control: string; kind: "button"; pressed: boolean }
  | { type: "ack"; id: string; ok: boolean }
  | { type: "error"; code: string; message: string }
  | { type: string; [key: string]: unknown };

export function parseDeviceLine(line: string): DeviceMessage | null {
  if (!line.trim() || line.length > 512) return null;
  let parsed: unknown;
  try { parsed = JSON.parse(line); } catch { return null; }
  if (!parsed || typeof parsed !== "object" || !("type" in parsed)) return null;
  const message = parsed as Record<string, unknown>;
  if (typeof message.type !== "string") return null;
  if (message.type === "control" && message.kind === "potentiometer") {
    if (typeof message.control !== "string" || typeof message.value !== "number" ||
        typeof message.min !== "number" || typeof message.max !== "number" || message.max <= message.min) return null;
  }
  if (message.type === "control" && message.kind === "button") {
    if (typeof message.control !== "string" || typeof message.pressed !== "boolean") return null;
  }
  if (message.type === "hello") {
    if (typeof message.device !== "string" || typeof message.protocol !== "number" ||
        typeof message.firmware !== "string" || !Array.isArray(message.capabilities)) return null;
  }
  return message as DeviceMessage;
}

export function encodeRgbCommand(led: string, r: number, g: number, b: number, id: string): string {
  const clamp = (value: number) => Math.max(0, Math.min(255, Math.round(value)));
  return JSON.stringify({ type: "set_rgb", id, led, r: clamp(r), g: clamp(g), b: clamp(b) }) + "\n";
}
