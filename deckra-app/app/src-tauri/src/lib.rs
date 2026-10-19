use serde::Serialize;

#[derive(Serialize)]
struct PortInfo { name: String, description: String }

#[tauri::command]
fn list_serial_ports() -> Result<Vec<PortInfo>, String> {
    let ports = serialport::available_ports().map_err(|e| e.to_string())?;
    Ok(ports.into_iter().map(|port| {
        let description = match port.port_type {
            serialport::SerialPortType::UsbPort(info) => format!("USB VID:{:04X} PID:{:04X}", info.vid, info.pid),
            serialport::SerialPortType::BluetoothPort => "Bluetooth".to_string(),
            serialport::SerialPortType::PciPort => "PCI".to_string(),
            serialport::SerialPortType::Unknown => "Serial".to_string(),
        };
        PortInfo { name: port.port_name, description }
    }).collect())
}

#[tauri::command]
fn test_serial_connection(port_name: String, baud_rate: u32) -> Result<String, String> {
    if baud_rate == 0 || baud_rate > 1_000_000 { return Err("Baud rate inválido".to_string()); }
    let port = serialport::new(&port_name, baud_rate)
        .timeout(std::time::Duration::from_millis(300))
        .open().map_err(|e| format!("Não foi possível abrir {}: {}", port_name, e))?;
    drop(port);
    Ok(format!("Acesso à porta {} validado", port_name))
}

#[tauri::command]
fn send_serial_line(line: String) -> Result<(), String> {
    if line.len() > 512 || line.contains('\n') || line.contains('\r') {
        return Err("Mensagem inválida ou excede o limite permitido".to_string());
    }
    Err("Envio serial ainda não habilitado no scaffold".to_string())
}

#[cfg_attr(mobile, tauri::mobile_entry_point)]
pub fn run() {
    tauri::Builder::default()
        .invoke_handler(tauri::generate_handler![list_serial_ports, test_serial_connection, send_serial_line])
        .run(tauri::generate_context!())
        .expect("erro ao iniciar DECKRA Control Center");
}
