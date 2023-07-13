import http from 'k6/http';
import {URLSearchParams} from 'https://jslib.k6.io/url/1.0.0/index.js';
import {check, sleep} from 'k6';
import {Counter} from 'k6/metrics';

export const epDataSent = new Counter('data_sent_endpoint');
export const epDataRecv = new Counter('data_received_endpoint');

function sizeOfHeaders(headers) {
    return Object.keys(headers).reduce((sum, key) => sum + key.length + headers[key].length, 0);
}

function trackDataMetricsPerURL(res) {
    epDataSent.add(sizeOfHeaders(res.request.headers) + res.request.body.length, { url: res.url });
    epDataRecv.add(sizeOfHeaders(res.headers) + res.body.length, { url: res.url });
}


const baseURL = '[(${baseURL})]';

export const options = [[${options}]];

const payloads = [(${payloads}})];
// /*[# th:each="payload : ${payloads}"]*/
// payloads.push([(${payload.trim()})]);
// /*[/]*/

const searchParams = [($serachParams)];

const path_variables = [(${pathVariables})];


export default function() {

}
