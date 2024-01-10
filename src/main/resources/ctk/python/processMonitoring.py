from datetime import datetime
import time
import psutil

import influxDBConnector


def check_process_exists(process_name, log_result_in_influx_db):
    print("checking " + datetime.now().strftime("%Y-%m-%d %H:%M:%S"))
    for process in psutil.process_iter(['pid', 'name']):
        # print(process)
        if process.info['name'] == process_name:
            if log_result_in_influx_db:
                influxDBConnector.write_monitoring_data("process_running", "process_name", process_name, "is_running",
                                                        True)
            return True

    if log_result_in_influx_db:
        influxDBConnector.write_monitoring_data("process_running", "process_name", process_name, "is_running", False)
    return False


def get_duration_until_process_started(process_name, monitoring_duration_sec, checking_interval_sec):
    monitoring_start_time = datetime.now()
    # print("monitoring_start_time", monitoring_start_time)

    while (datetime.now() - monitoring_start_time).total_seconds() < monitoring_duration_sec:
        if check_process_exists(process_name, True):
            duration_until_process_started = datetime.now() - monitoring_start_time
            print(f"The process '{process_name}' exists. It took {duration_until_process_started} to restart.")
            return duration_until_process_started.total_seconds()  # Exit the loop if the process is found

        # Wait for a short interval before checking again
        # Is currently technically limited to precision of max. 1-2 checks per second
        time.sleep(checking_interval_sec)

    else:
        print(f"The process '{process_name}' was not found within {monitoring_duration_sec} seconds.")
        return None


# get_duration_until_process_started("KeePassXC.exe", 5, 0)


influxDBConnector.write_monitoring_data("process_running", "process_name", "einProzess.exe", "is_running",
                                                        True)
time.sleep(1)
influxDBConnector.write_monitoring_data("process_running", "process_name", "einProzess.exe", "is_running",
                                        True)
time.sleep(1)
influxDBConnector.write_monitoring_data("process_running", "process_name", "einProzess.exe", "is_running",
                                        False)
time.sleep(1)
influxDBConnector.write_monitoring_data("process_running", "process_name", "einProzess.exe", "is_running",
                                        False)
time.sleep(1)
influxDBConnector.write_monitoring_data("process_running", "process_name", "einProzess.exe", "is_running",
                                        True)