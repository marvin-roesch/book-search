import hljs from 'highlight.js/lib/highlight';
import hljsCss from 'highlight.js/lib/languages/css';

export default {
  install(Vue) {
    hljs.registerLanguage('css', hljsCss);
    Vue.directive('closable', {
      bind(el, binding, vnode) {
        el.handleOutsideClick = (e) => {
          e.stopPropagation();
          const { handler, exclude } = binding.value;

          let clickedOnExcludedEl = false;
          exclude.forEach((refName) => {
            if (!clickedOnExcludedEl) {
              const excludedEl = vnode.context.$refs[refName];
              clickedOnExcludedEl = excludedEl.contains(e.target);
            }
          });
          if (!el.contains(e.target) && !clickedOnExcludedEl) {
            handler();
          }
        };

        document.addEventListener('click', el.handleOutsideClick);
        document.addEventListener('touchstart', el.handleOutsideClick);
      },
      unbind(el) {
        document.removeEventListener('click', el.handleOutsideClick);
        document.removeEventListener('touchstart', el.handleOutsideClick);
      },
    });

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
  },
};
