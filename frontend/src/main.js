import Vue from 'vue';
import * as shadow from 'vue-shadow-dom';
import VueHighlightJS from 'vue-highlightjs';
import App from './App.vue';
import router from './router';

Vue.use(shadow);
Vue.use(VueHighlightJS);

Vue.config.productionTip = false;

new Vue({
  router,
  render: h => h(App),
}).$mount('#app');
