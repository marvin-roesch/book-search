<template>
<div class="library-overview">
  <UserPanel></UserPanel>
  <ul class="library-overview-quicklinks">
    <li v-for="s in series" :key="s.name">
      <a :href="`#${s.name}`">{{ s.name }}</a>
    </li>
  </ul>
  <transition-group name="fade-slide-up">
    <SeriesTree :series="s" :include-anchor="true" v-for="s in series" :key="s.name"></SeriesTree>
  </transition-group>
</div>
</template>

<script>
import { mapState } from 'vuex';
import SeriesTree from '@/components/read/SeriesTree.vue';
import UserPanel from '@/components/UserPanel.vue';

let updaterTimeout = null;

function transitionListener(event) {
  const cover = event.target;
  const coverDummy = document.getElementById(`book-${cover.dataset.bookId}-cover-dummy`);
  cover.style.position = '';
  cover.style.width = '';
  cover.style.height = '';
  cover.style.top = '';
  cover.style.left = '';
  cover.style.display = '';
  if (coverDummy !== null) {
    coverDummy.parentNode.replaceChild(cover, coverDummy);
  }
  cover.removeEventListener('transitionend', transitionListener);
}

export default {
  name: 'library',
  components: { UserPanel, SeriesTree },
  computed: mapState(['series']),
  async mounted() {
    try {
      await this.$store.dispatch(
        'refreshSeries',
        { seriesFilter: null, bookFilter: null, excluded: null },
      );
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
            cover.style.display = '';
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
              if (!from.meta.coverTransition) {
                transitionListener({ target: cover });
              } else {
                cover.addEventListener('transitionend', transitionListener);
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
      const coverDummy = document.getElementById(`book-${cover.dataset.bookId}-cover-dummy`);
      if (coverDummy !== null) {
        cover.style.position = '';
        cover.style.width = '';
        cover.style.height = '';
        cover.style.top = '';
        cover.style.left = '';
        coverDummy.parentNode.replaceChild(cover, coverDummy);
      }
      cover.removeEventListener('transitionend', transitionListener);
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
    }
    next();
  },
};
</script>

<style lang="scss">
.library-overview {
  box-sizing: border-box;
  padding: 0.5rem 2rem 2rem;
  width: 100%;

  &-quicklinks {
    list-style-type: none;
    padding: 0 0 0 0.5rem;
    margin: 0;

    &:before {
      content: 'Jump to:';
      margin-right: 0.5rem;
    }

    display: flex;
    flex-wrap: wrap;

    li {
      margin-right: 0.25rem;

      &:after {
        display: inline-block;
        content: "\B7";
        margin-left: 0.25rem;
      }

      &:last-child:after {
        content: '';
      }
    }
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

    &-quicklinks {
      margin-top: 0.5rem;
    }
  }
}
</style>
