from datetime import datetime
import time

import requests
import influxDBConnector

base_url = "http://host.docker.internal:443"
request_timeout_sec = 5


def request_check_process_exists(process_name, log_result_in_influx_db):
    url = f"{base_url}/check_process?name={process_name}"
    print("request process checking: " + url + " " + datetime.now().strftime("%Y-%m-%d %H:%M:%S"))

    try:
        response = requests.get(url, timeout=request_timeout_sec)

        if response.status_code == 200 and response.json().get(f'{process_name} exists') == True:
            print(f"{response.status_code} - {response.json()}")
            if log_result_in_influx_db:
                influxDBConnector.write_monitoring_data("process_running", "process_name", process_name, "is_running",
                                                        True)
            return True

        if response.status_code == 200 and response.json().get(f'{process_name} exists') == False:
            print(f"{response.status_code} - {response.json()}")
            if log_result_in_influx_db:
                influxDBConnector.write_monitoring_data("process_running", "process_name", process_name, "is_running",
                                                        False)
            return False

        else:
            print(f"Error: {response.status_code}")

            return False

    except requests.Timeout:
        print("Request timed out")


def get_duration_until_process_started(process_name, monitoring_duration_sec, checking_interval_sec):
    print(
        f"started monitoring until process {process_name} (re)starts: " + datetime.now().strftime("%Y-%m-%d %H:%M:%S"))

    monitoring_start_time = datetime.now()
    # print("monitoring_start_time", monitoring_start_time)

    while (datetime.now() - monitoring_start_time).total_seconds() < monitoring_duration_sec:
        if request_check_process_exists(process_name, True):
            duration_until_process_started = datetime.now() - monitoring_start_time
            print(f"The process '{process_name}' exists. It took {duration_until_process_started} to restart.")
            return duration_until_process_started.total_seconds()  # Exit the loop if the process is found

        # Wait for a short interval before checking again
        # Is currently technically limited to precision of max. 1-2 checks per second
        time.sleep(checking_interval_sec)

    else:
        print(f"The process '{process_name}' was not found within {monitoring_duration_sec} seconds.")
        return None


def request_kill_process_by_name(process_name):
    url = f"{base_url}/kill_process?name={process_name}"
    print("request kill process: " + url + " " + datetime.now().strftime("%Y-%m-%d %H:%M:%S"))

    try:
        response = requests.post(url, timeout=request_timeout_sec)

        if response.status_code == 200:
            print(f"{response.status_code} - {response.json()}")
            return True

        else:
            print(f"Error: {response.status_code} - {response.json()}")
            return False

    except requests.Timeout:
        print("Request timed out")


def request_start_process_by_path(path):
    # TODO if case study evaluates that this is desired.
    print("Please restart the process manually")
