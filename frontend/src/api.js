import axios from 'axios';

const BASE_URL = '/api';

const client = axios.create({
  baseURL: BASE_URL,
});
client.login = () => {
};
client.interceptors.response.use(
  response => response,
  (error) => {
    if (axios.isCancel(error)) {
      return Promise.reject(error);
    }

    const { response: { status } } = error;

    if (status === 401) {
      client.login();
    }

    return Promise.reject(error);
  },
);

export default client;
