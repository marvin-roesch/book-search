import client from '@/api';
import router from '@/router';
import store from '@/store';

export default {
  install(Vue) {
    client.login = () => {
      store.commit('resetIdentity');
      router.replace({ name: 'login', query: { redirect: router.currentRoute.fullPath } });
    };

    Object.defineProperty(Vue.prototype, '$api', {
      get() {
        return client;
      },
    });
  },
};
