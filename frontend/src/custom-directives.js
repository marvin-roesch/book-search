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

    const getApiError = (error) => {
      if (error.response) {
        const { status, data } = error.response;
        if (data.message) {
          return data.message;
        } else {
          switch (status) {
            case 400:
              return 'The provided data is invalid!';
            case 401:
              return 'Only logged in users may do this!';
            case 403:
              return 'You do not have permission to do this!';
            default:
              return null;
          }
        }
      }
      return null;
    };

    Vue.prototype.$getApiError = getApiError;

    Vue.prototype.$handleApiError = (error) => {
      const message = getApiError(error);
      if (message !== null) {
        store.dispatch(
          'notifications/push',
          { type: 'error', message },
        );
        return;
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
