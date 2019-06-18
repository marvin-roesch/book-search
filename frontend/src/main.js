import Vue from 'vue';
import * as shadow from 'vue-shadow-dom';
import hljs from 'highlight.js/lib/highlight';
import hljsCss from 'highlight.js/lib/languages/css';
import App from './App.vue';
import router from './router';
import store from './store';

hljs.registerLanguage('css', hljsCss);

Vue.use(shadow);

Vue.directive('highlightjs', {
  deep: true,
  bind(el, binding) {
    const targets = el.querySelectorAll('code');
    targets.forEach((target) => {
      if (binding.value) {
        target.textContent = binding.value;
      }
      hljs.highlightBlock(target);
    });
  },
  componentUpdated(el, binding) {
    const targets = el.querySelectorAll('code');
    targets.forEach((target) => {
      if (binding.value) {
        target.textContent = binding.value;
        hljs.highlightBlock(target);
      }
    });
  },
});

Vue.config.productionTip = false;

new Vue({
  router,
  store,
  render: h => h(App),
}).$mount('#app');
