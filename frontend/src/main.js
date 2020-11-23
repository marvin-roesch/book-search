import Vue from 'vue';
import * as shadow from 'vue-shadow-dom';
import InfiniteScroll from 'vue-infinite-scroll';
import Vuebar from 'vuebar';
import App from '@/App.vue';
import router from '@/router';
import store from '@/store';
import Api from '@/api-plugin';
import CustomDirectives from '@/custom-directives';

Vue.use(Api);
Vue.use(CustomDirectives);
Vue.use(shadow);
Vue.use(InfiniteScroll);
Vue.use(Vuebar);

Vue.config.productionTip = false;

new Vue({
  router,
  store,
  render: h => h(App),
}).$mount('#app');
