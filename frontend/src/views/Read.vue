<template>
<div class="read-overview">
  <UserPanel></UserPanel>
  <transition-group name="fade-slide-up">
    <SeriesTree :series="s" v-for="s in series" :key="s.name"></SeriesTree>
  </transition-group>
</div>
</template>

<script>
import SeriesTree from '@/components/read/SeriesTree.vue';
import UserPanel from '@/components/UserPanel.vue';

let updaterTimeout = null;

function postTransitionHook(cover, coverDummy) {
  return () => {
    cover.style.position = '';
    cover.style.width = '';
    cover.style.height = '';
    cover.style.top = '';
    cover.style.left = '';
    coverDummy.parentNode.replaceChild(cover, coverDummy);
    cover.removeEventListener('transitionend', cover.transitionListener);
    cover.transitionListener = undefined;
  };
}

export default {
  name: 'read',
  components: { UserPanel, SeriesTree },
  data() {
    return {
      series: [],
    };
  },
  async mounted() {
    try {
      const { data: allSeries } = await this.$api.get('/books/series');
      this.series = allSeries;
    } catch (error) {
      this.$handleApiError(error);
    }
  },
  beforeRouteEnter(to, from, next) {
    next(() => {
      updaterTimeout = setTimeout(() => {
        const covers = document.querySelectorAll('body > div[data-book-id]');
        covers.forEach((cover) => {
          const coverDummy = document.getElementById(`book-${cover.dataset.bookId}-cover-dummy`);
          if (coverDummy !== null) {
            const rect = coverDummy.getBoundingClientRect();
            cover.classList.add('no-transition');
            cover.style.top = `${cover.offsetTop + window.scrollY}px`;
            cover.style.position = 'absolute';
            cover.readTransitionTimeout = setTimeout(() => {
              cover.classList.remove('no-transition');
              cover.style.top = `${rect.top + window.scrollY}px`;
              cover.style.left = `${rect.left}px`;
              cover.style.width = `${rect.width}px`;
              cover.style.height = `${rect.height}px`;
              cover.style.maxWidth = '';
              cover.style.maxHeight = '';
              cover.transitionListener = postTransitionHook(cover, coverDummy);
              if (cover.noTransition) {
                cover.transitionListener();
                cover.transitionListener = undefined;
              } else {
                cover.addEventListener('transitionend', cover.transitionListener);
              }
            }, 50);
          }
        });
      }, 50);
    });
  },
  beforeRouteLeave(to, from, next) {
    const covers = document.querySelectorAll('body > div[data-book-id]');
    clearTimeout(updaterTimeout);
    covers.forEach((cover) => {
      if (cover.transitionListener) {
        cover.removeEventListener('transitionend', cover.transitionListener);
        cover.transitionListener = undefined;
      }
      if (cover.readTransitionTimeout) {
        clearTimeout(cover.readTransitionTimeout);
        cover.readTransitionTimeout = undefined;
      }
    });

    if (to.meta.coverTransition) {
      const cover = document.getElementById(`book-${to.params.id}-cover`);
      const bounds = cover.getBoundingClientRect();
      const dummy = document.createElement('div');
      dummy.id = `book-${to.params.id}-cover-dummy`;
      dummy.style.width = `${bounds.width}px`;
      dummy.style.height = `${bounds.height}px`;
      dummy.classList.add('book-cover-dummy');
      cover.parentNode.prepend(dummy);
      document.body.appendChild(cover);
      cover.style.position = 'fixed';
      cover.style.top = `${bounds.top}px`;
      cover.style.left = `${bounds.left}px`;
      cover.style.width = `${bounds.width}px`;
      cover.style.height = `${bounds.height}px`;
    } else {
      const covers = document.querySelectorAll('body > div[data-book-id]');
      covers.forEach((cover) => {
        const coverDummy = document.getElementById(`book-${cover.dataset.bookId}-cover-dummy`);
        if (coverDummy !== null) {
          cover.style.position = '';
          cover.style.width = '';
          cover.style.height = '';
          cover.style.top = '';
          cover.style.left = '';
          coverDummy.parentNode.replaceChild(cover, coverDummy);
        }
      });
    }
    next();
  },
};
</script>

<style lang="scss">
.read-overview {
  box-sizing: border-box;
  padding: 0.5rem 2rem 2rem;
  width: 100%;

  .user-panel {
  }

  .expandable-content {
    padding-left: 0;
    padding-right: 0;
  }

  @media (max-width: 1200px) {
    padding: 1rem;

    .user-panel {
      padding: 0;
    }
  }
}
</style>
