import influxdb_client, os, time
from influxdb_client import InfluxDBClient, Point, WritePrecision
from influxdb_client.client.write_api import SYNCHRONOUS

token = "6DPUgkuluiglJehGcOIalA8advCMp7KM6i69o_rtcPloBy9y8XX_Uks8vAdzVAaz9uJmDox96tjOSYSWAdWznw==" # os.environ.get("INFLUXDB_TOKEN")
org = "Dqualizer"
url = "http://localhost:7887"

write_client = influxdb_client.InfluxDBClient(url=url, token=token, org=org)

bucket = "ResilienceMonitoringData"

write_api = write_client.write_api(write_options=SYNCHRONOUS)

for value in range(5):
    point = (
        Point("measurement1")
        .tag("tagname1", "tagvalue1")
        .field("field1", value)
    )
    write_api.write(bucket=bucket, org="Dqualizer", record=point)
    time.sleep(1)  # separate points by 1 second



# Get single Values
query_api = write_client.query_api()

query = """from(bucket: "ResilienceMonitoringData")
 |> range(start: -10m)
 |> filter(fn: (r) => r._measurement == "measurement1")"""
tables = query_api.query(query, org="Dqualizer")

for table in tables:
  for record in table.records:
    print(record)


# Get mean
query_api = write_client.query_api()

query = """from(bucket: "ResilienceMonitoringData")
  |> range(start: -10m)
  |> filter(fn: (r) => r._measurement == "measurement1")
  |> mean()"""
tables = query_api.query(query, org="Dqualizer")

for table in tables:
    for record in table.records:
        print(record)

