import hljs from 'highlight.js/lib/core';
import hljsCss from 'highlight.js/lib/languages/css';
import axios from 'axios';
import store from '@/store';

export const scrollAware = {
  data() {
    return {
      scrolledDown: false,
      lastScrollPosition: 0,
    };
  },
  mounted() {
    window.addEventListener('scroll', this.onScroll);
  },
  beforeDestroy() {
    window.removeEventListener('scroll', this.onScroll);
  },
  methods: {
    onScroll() {
      const currentScrollPosition = window.pageYOffset || document.documentElement.scrollTop;
      if (currentScrollPosition < 0) {
        return;
      }
      this.scrolledDown = currentScrollPosition >= this.lastScrollPosition;
      this.lastScrollPosition = currentScrollPosition;
    },
  },
};

export default {
  install(Vue) {
    hljs.registerLanguage('css', hljsCss);
    Vue.directive('closable', {
      bind(element, binding, vnode) {
        element.handleOutsideClick = (e) => {
          e.stopPropagation();
          const { handler, exclude } = binding.value;

          const clickedOnExcludedEl = (exclude || [])
            .reduce(
              (acc, refName) => acc || vnode.context.$refs[refName].contains(e.target),
              false,
            );
          if (!element.contains(e.target) && !clickedOnExcludedEl) {
            handler();
          }
        };

        document.addEventListener('click', element.handleOutsideClick);
        document.addEventListener('touchstart', element.handleOutsideClick);
      },
      unbind(element) {
        document.removeEventListener('click', element.handleOutsideClick);
        document.removeEventListener('touchstart', element.handleOutsideClick);
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
        }
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
      return null;
    };

    Vue.prototype.$getApiError = getApiError;

    Vue.prototype.$handleApiError = (error) => {
      if (axios.isCancel(error)) {
        return;
      }

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
