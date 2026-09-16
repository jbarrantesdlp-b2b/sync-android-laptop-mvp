param(
  [Parameter(Mandatory = $true)][string]$Ip,
  [int]$Port = 8123
)

$ErrorActionPreference = 'Stop'
try {
  [Windows.Devices.Bluetooth.Advertisement.BluetoothLEAdvertisementPublisher, Windows.Devices.Bluetooth, ContentType = WindowsRuntime] | Out-Null
  [Windows.Storage.Streams.DataWriter, Windows.Storage.Streams, ContentType = WindowsRuntime] | Out-Null

  $publisher = New-Object Windows.Devices.Bluetooth.Advertisement.BluetoothLEAdvertisementPublisher
  $writer = New-Object Windows.Storage.Streams.DataWriter
  $octets = $Ip.Split('.') | ForEach-Object { [byte]$_ }
  if ($octets.Count -ne 4) { throw "IP invalida $Ip" }
  $hi = [byte](($Port -shr 8) -band 255)
  $lo = [byte]($Port -band 255)
  $bytes = [byte[]]@(0x53, 0x45) + [byte[]]$octets + @($hi, $lo)
  $writer.WriteBytes($bytes)
  $mfg = New-Object Windows.Devices.Bluetooth.Advertisement.BluetoothLEManufacturerData
  $mfg.CompanyId = 0xFFFF
  $mfg.Data = $writer.DetachBuffer()
  $publisher.Advertisement.ManufacturerData.Add($mfg)
  $publisher.Advertisement.LocalName = "SYNC-ENG"
  $publisher.Start()
  while ($true) { Start-Sleep -Seconds 20 }
} catch {
  exit 1
}
