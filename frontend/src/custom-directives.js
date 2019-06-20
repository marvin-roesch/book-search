import hljs from 'highlight.js/lib/highlight';
import hljsCss from 'highlight.js/lib/languages/css';
import store from '@/store';

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

    Vue.prototype.$handleApiError = (error) => {
      if (error.response) {
        const { status, data } = error.response;
        if (data.message) {
          store.dispatch(
            'notifications/push',
            { type: 'error', message: data.message },
          );
          return;
        } else {
          switch (status) {
            case 400:
              store.dispatch(
                'notifications/push',
                { type: 'error', message: 'The provided data is invalid!' },
              );
              return;
            case 401:
              store.dispatch(
                'notifications/push',
                { type: 'error', message: 'Only logged in users may do this!' },
              );
              return;
            case 403:
              store.dispatch(
                'notifications/push',
                { type: 'error', message: 'You do not have permission to do this!' },
              );
              return;
          }
        }
      }
      console.error(error);
      store.dispatch(
        'notifications/push',
        { type: 'error', message: 'An unknown error occurred. Please report this!' },
      );
    };

    Object.defineProperty(Vue.prototype, '$notifications', {
      get() {
        return {
          success(message) {
            store.dispatch('notifications/push', { type: 'success', message });
          },
          error(message) {
            store.dispatch('notifications/push', { type: 'error', message });
          },
        };
      },
    });
  },
};
