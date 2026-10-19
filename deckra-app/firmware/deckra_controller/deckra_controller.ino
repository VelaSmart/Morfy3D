/*
 * DECKRA firmware scaffold for Arduino Leonardo / Micro (ATmega32U4).
 * USB Serial/CDC, JSON Lines protocol prototype.
 * Confirm actual wiring before connecting components.
 */
#include <Arduino.h>
#include <string.h>

static const unsigned long SERIAL_BAUD = 115200;
static const unsigned long REPORT_INTERVAL_MS = 50;
static unsigned long lastReport = 0;

// Prototype pin placeholders only. Confirm the final schematic before wiring.
static const uint8_t POT_COUNT = 2;
static const uint8_t BUTTON_COUNT = 2;
static const uint8_t POT_PINS[POT_COUNT] = {A0, A1};
static const uint8_t BUTTON_PINS[BUTTON_COUNT] = {2, 3};
static char inputLine[160];
static size_t inputLength = 0;

void sendHello() {
  Serial.println(F("{\"type\":\"hello\",\"protocol\":1,\"device\":\"deckra\",\"firmware\":\"0.1.0\",\"capabilities\":[\"potentiometers\",\"buttons\",\"rgb\"]}"));
}
void processCommand(const char* line) {
  if (strcmp(line, "{\"type\":\"ping\"}") == 0) {
    Serial.println(F("{\"type\":\"ack\",\"id\":\"ping\",\"ok\":true}"));
    return;
  }
  Serial.println(F("{\"type\":\"error\",\"code\":\"unsupported_command\",\"message\":\"Command not enabled in firmware scaffold\"}"));
}
void setup() {
  Serial.begin(SERIAL_BAUD);
  for (uint8_t i=0; i<BUTTON_COUNT; ++i) pinMode(BUTTON_PINS[i], INPUT_PULLUP);
  // RGB output intentionally disabled until LED type and pin mapping are confirmed.
  sendHello();
}
void loop() {
  while (Serial.available() > 0) {
    const char c = static_cast<char>(Serial.read());
    if (c == '\r') continue;
    if (c == '\n') {
      inputLine[inputLength] = '\0';
      processCommand(inputLine);
      inputLength = 0;
    } else if (inputLength < sizeof(inputLine)-1) {
      inputLine[inputLength++] = c;
    } else {
      inputLength = 0;
      Serial.println(F("{\"type\":\"error\",\"code\":\"line_too_long\",\"message\":\"Input line exceeded limit\"}"));
    }
  }
  const unsigned long now = millis();
  if (now-lastReport >= REPORT_INTERVAL_MS) {
    lastReport = now;
    for (uint8_t i=0; i<POT_COUNT; ++i) {
      Serial.print(F("{\"type\":\"control\",\"control\":\"pot_")); Serial.print(i+1);
      Serial.print(F("\",\"kind\":\"potentiometer\",\"value\":")); Serial.print(analogRead(POT_PINS[i]));
      Serial.println(F(",\"min\":0,\"max\":1023}"));
    }
    for (uint8_t i=0; i<BUTTON_COUNT; ++i) {
      Serial.print(F("{\"type\":\"control\",\"control\":\"button_")); Serial.print(i+1);
      Serial.print(F("\",\"kind\":\"button\",\"pressed\":"));
      Serial.print(digitalRead(BUTTON_PINS[i])==LOW ? F("true") : F("false"));
      Serial.println('}');
    }
  }
}
