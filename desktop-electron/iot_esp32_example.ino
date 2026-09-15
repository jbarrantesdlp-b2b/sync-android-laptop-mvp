/*
 * Sync Engine — ESP32 IoT node
 * Publica telemetria al dashboard de la laptop (mismo protocolo que el telefono).
 *
 * 1. Edita WIFI_SSID, WIFI_PASS y LAPTOP_IP
 * 2. Flash con Arduino IDE (placa ESP32) o PlatformIO
 * 3. En la laptop: npm start en desktop-electron
 * 4. Los tiles IoT se actualizan cada 3s
 *
 * Endpoint: POST http://LAPTOP_IP:8123/api/iot
 * Tambien puedes usar ws://LAPTOP_IP:8123 con el mismo JSON.
 */

#include <WiFi.h>
#include <HTTPClient.h>

const char* WIFI_SSID = "TU_WIFI";
const char* WIFI_PASS = "TU_PASSWORD";
const char* LAPTOP_IP = "192.168.1.10";
const int   LAPTOP_PORT = 8123;
const char* DEVICE_NAME = "ESP32-DevKit";

void setup() {
  Serial.begin(115200);
  WiFi.mode(WIFI_STA);
  WiFi.begin(WIFI_SSID, WIFI_PASS);
  Serial.print("WiFi");
  while (WiFi.status() != WL_CONNECTED) {
    delay(400);
    Serial.print(".");
  }
  Serial.println();
  Serial.print("IP ESP32: ");
  Serial.println(WiFi.localIP());
}

void loop() {
  if (WiFi.status() == WL_CONNECTED) {
    // Sustituye estos valores por lecturas reales (DHT22, BH1750, BMP280, etc.)
    float tempC = 24.5;
    float humidity = 48.0;
    float pressure = 1013.2;

    String body = String("{\"id\":\"esp32-1\",\"type\":\"IOT_TELEMETRY\",\"payload\":{") +
      "\"source\":\"esp32\"," +
      "\"device\":\"" + DEVICE_NAME + "\"," +
      "\"sensors\":{" +
      "\"lightLux\":null," +
      "\"accelG\":null," +
      "\"proximityCm\":null," +
      "\"batteryPct\":null," +
      "\"charging\":false," +
      "\"steps\":null," +
      "\"tempC\":" + String(tempC, 2) + "," +
      "\"humidity\":" + String(humidity, 2) + "," +
      "\"pressureHpa\":" + String(pressure, 2) + "," +
      "\"lat\":null," +
      "\"lng\":null" +
      "},\"available\":[\"temperatura\",\"humedad\",\"presion\"]}}";

    HTTPClient http;
    String url = String("http://") + LAPTOP_IP + ":" + LAPTOP_PORT + "/api/iot";
    http.begin(url);
    http.addHeader("Content-Type", "application/json");
    int code = http.POST(body);
    Serial.printf("POST /api/iot -> %d\n", code);
    http.end();
  }
  delay(3000);
}
