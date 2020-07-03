const fetch = require("node-fetch").default;
import download from "downloadjs";

const METHOD = { GET: "GET", POST: "POST", PUT: "PUT", DELETE: "DELETE" };

class Initialisation {
  static async get() {
    const headers = {
      Accept: "application/json",
      "Content-Type": "application/json",
    };

    const init = {
      headers: headers,
      mode: "cors",
      cache: "default",
    };

    return init;
  }
}

const http = {
  async get(url) {
    return await this.call(METHOD.GET, url);
  },
  async delete(url) {
    return await this.call(METHOD.DELETE, url);
  },
  async post(url, body, isStr) {
    return await this.call(METHOD.POST, url, body, isStr);
  },
  async put(url, body) {
    return await this.call(METHOD.PUT, url, body);
  },
  async download(url, fileName) {
    const res = await this.call(METHOD.GET, url);
    const blob = await res.blob();
    download(blob, fileName);
  },
  async call(method, url, body, isStr) {
    const init = await Initialisation.get();
    init.method = method;
    if (isStr) {
      init.body = body;
    } else {
      init.body = JSON.stringify(body);
    }
    const res = await fetch(url, init);
    if (!res.ok) {
      const json = await res.json();
      throw new Error(json.erreurs);
    }
    return res;
  },
};

export default http;
