import influxdb_client, os, time
from influxdb_client import InfluxDBClient, Point, WritePrecision
from influxdb_client.client.write_api import SYNCHRONOUS


token = "specialToken000"
org = "my-org"
url = "http://localhost:8086"

write_client = influxdb_client.InfluxDBClient(url=url, token=token, org=org)

bucket = "my-bucket"

write_api = write_client.write_api(write_options=SYNCHRONOUS)
query_api = write_client.query_api()


def write_monitoring_data(measurement_id, tag_name, tag_value, field, value):
    point = (
        Point(measurement_id)
        .tag(tag_name, tag_value)
        .field(field, value)
    )
    write_api.write(bucket=bucket, org=org, record=point)