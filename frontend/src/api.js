import axios from 'axios';
import router from '@/router';
import store from '@/store';

const BASE_URL = '/api';

export default {
  install(Vue) {
    const client = axios.create({
      baseURL: BASE_URL,
    });
    client.interceptors.response.use(
      response => response,
      (error) => {
        if (axios.isCancel(error)) {
          return Promise.reject(error);
        }

        const { response: { status } } = error;

        if (status === 401) {
          store.commit('resetIdentity');
          router.replace({ name: 'login', query: { redirect: router.currentRoute.fullPath } });
        }

        return Promise.reject(error);
      },
    );

    Object.defineProperty(Vue.prototype, '$api', {
      get() {
        return client;
      },
    });
  },
};
