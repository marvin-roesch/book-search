<template>
<div :class="{
    'book-overview': true,
    'book-overview-scrolled': scrolledDown
  }">
  <div class="book-overview-header">
    <UserPanel></UserPanel>
    <h1>{{ $route.meta.title }}</h1>
    <XIcon class="book-overview-close-icon" @click.prevent.stop="$router.push({ name: 'library' })">
    </XIcon>
  </div>
  <div class="book-overview-sidebar">
    <div class="book-cover" ref="cover">
      <div v-show="hasOldCover && !hasCover" ref="coverDummy"></div>
      <transition
        :name="fromOldCover ? '' : 'fade-slide-up'">
        <img :src="`/api/books/${bookId}/cover`" v-show="!hasOldCover && hasCover">
      </transition>
    </div>
    <transition name="fade-slide-up">
      <div class="book-metadata" v-if="book !== null">
        <div class="book-overview-title">
          {{ book.title }}
        </div>
        <div class="book-overview-author">
          {{ book.author }}
        </div>
      </div>
    </transition>
    <transition-group tag="ul" class="book-overview-nav" name="fade-slide-up">
      <li :key="'search'" v-if="book !== null">
        <router-link
          :to="{ name: 'search', query: { focus: 'true', q: '', series: '', books: bookId } }">
          <SearchIcon></SearchIcon>
          Search
        </router-link>
      </li>
      <li :key="'chapters'" v-if="book !== null">
        <router-link :to="{ name: 'book-chapters', params: { id: bookId } }" exact>
          <ListIcon></ListIcon>
          Chapters
        </router-link>
      </li>
      <li :key="'dictionary'" v-if="book !== null">
        <router-link :to="{ name: 'book-dictionary', params: { id: bookId } }" exact>
          <BookIcon></BookIcon>
          Dictionary
        </router-link>
      </li>
    </transition-group>
  </div>
  <h1>{{ $route.meta.title }}</h1>
  <transition name="fade">
    <router-view></router-view>
  </transition>
</div>
</template>

<script>
import UserPanel from '@/components/UserPanel.vue';
import { BookIcon, ListIcon, SearchIcon, XIcon } from 'vue-feather-icons';
import { scrollAware } from '@/custom-directives';

export default {
  name: 'book-overview',
  mixins: [scrollAware],
  components: {
    SearchIcon, BookIcon, ListIcon, UserPanel, XIcon,
  },
  data() {
    return {
      bookId: '',
      book: null,
      chapters: [],
      hasCover: false,
      hasOldCover: false,
      fromOldCover: false,
    };
  },
  mounted() {
    this.bookId = this.$route.params.id;
    this.loadBook();
    this.checkCover();
  },
  methods: {
    async loadBook() {
      try {
        const { data: book } = await this.$api.get(`/books/${this.bookId}`);
        this.book = book;
      } catch (error) {
        this.$handleApiError(error);
      }
    },
    async checkCover() {
      try {
        await this.$api.get(`/books/${this.bookId}/cover`);
        this.hasCover = true;
      } catch (error) {
        if (error.response && error.response.status === 404) {
          this.hasCover = false;
        } else {
          this.$handleApiError(error);
        }
      }
    },
  },
  beforeRouteEnter(to, from, next) {
    next((vm) => {
      const oldCover = document.getElementById(`book-${to.params.id}-cover`);
      const { cover } = vm.$refs;
      const styles = window.getComputedStyle(cover);
      if (oldCover !== null) {
        if (styles.getPropertyValue('display') === 'none') {
          oldCover.style.display = 'none';
          return;
        }
        const bounds = cover.getBoundingClientRect();
        const oldBounds = oldCover.getBoundingClientRect();
        const aspect = oldBounds.height / oldBounds.width;
        oldCover.style.top = `${bounds.top}px`;
        oldCover.style.left = `${bounds.left}px`;
        oldCover.style.width = `${bounds.width}px`;
        oldCover.style.height = `${aspect * bounds.width}px`;
        oldCover.style.maxHeight = oldCover.style.height;
        vm.$refs.coverDummy.style.width = oldCover.style.width;
        vm.$refs.coverDummy.style.height = oldCover.style.height;

        oldCover.addEventListener(
          'transitionend',
          function listener() {
            vm.hasOldCover = false;
            oldCover.style.display = 'none';
            oldCover.removeEventListener('transitionend', listener);
          },
        );
      }
      vm.fromOldCover = from.name === 'library' && oldCover !== null;
      vm.hasOldCover = vm.fromOldCover;
    });
  },
  beforeRouteLeave(to, from, next) {
    const oldCover = document.getElementById(`book-${this.bookId}-cover`);
    const { cover } = this.$refs;
    const styles = window.getComputedStyle(cover);
    if (oldCover !== null) {
      oldCover.style.display = to.name === 'library' ? '' : 'none';
      this.hasCover = false;
      if (styles.getPropertyValue('display') === 'none') {
        oldCover.noTransition = true;
        next();
        return;
      }
      const bounds = cover.getBoundingClientRect();
      oldCover.style.top = `${bounds.top}px`;
      oldCover.style.left = `${bounds.left}px`;
    }
    next();
  },
};
</script>

<style lang="scss">
.book-overview {
  box-sizing: border-box;
  padding: 5rem 0 0 24rem;
  width: 100%;

  &-header {
    position: fixed;
    display: flex;
    align-items: center;
    top: 0;
    left: 0;
    right: 0;
    background: white;
    box-shadow: 0 0 1rem rgba(0, 0, 0, 0.1);
    z-index: 1001;
    padding: 0.5rem 2rem 0.5rem;
    transition: 0.2s top ease-out;

    h1 {
      position: absolute;
      margin: 0;
      left: 26rem;
      font-size: 1.5rem;
    }

    .book-overview-close-icon {
      cursor: pointer;
      margin-left: auto;
    }
  }

  &-sidebar {
    position: fixed;
    top: 5rem;
    left: 0;
    bottom: 0;
    display: flex;
    flex-direction: column;
    align-items: stretch;
    box-sizing: border-box;
    border-bottom: 1px solid rgba(0, 0, 0, 0.1);
    box-shadow: 0 0 1rem rgba(0, 0, 0, 0.1);
    padding: 0.5rem 2rem 0;
    z-index: 1000;
    background: white;
    width: 24rem;
    transition: 0.2s top ease-out;
  }

  .book-metadata {
    transition-delay: 0.1s;
  }

  &-nav {
    display: flex;
    flex-direction: column;
    list-style-type: none;
    padding: 0;

    li {
      padding-bottom: 0.5rem;

      a {
        box-sizing: border-box;
        display: flex;
        align-items: center;
        padding: 0.25rem 0.5rem;

        .feather {
          margin-right: 0.5rem;
        }

        &.router-link-active {
          background: rgba(0, 0, 0, 0.05);
        }
      }
    }

    li:first-child {
      transition-delay: 0.3s;
    }

    li:nth-child(2) {
      transition-delay: 0.5s;
    }

    li:nth-child(3) {
      transition-delay: 0.7s;
    }
  }

  &-title {
    box-sizing: border-box;
    padding: 0.25rem 0 0;
    font-weight: bold;
    font-size: 1.25rem;
  }

  &-author {
    font-size: 0.8rem;
    color: lighten($base-text-color, 10%);
  }

  .book-cover {
    min-height: 18rem;
    width: 100%;
    max-width: 100%;
    margin-top: 1rem;

    img {
      max-width: 100%;
      max-height: 100%;
      display: block;
    }
  }

  & > h1 {
    display: none;
  }

  @media (max-width: $max-content-width) {
    padding-left: 0;
    padding-top: 7rem;

    & > h1 {
      display: block;
      font-size: 1.5rem;
      padding-left: 1rem;
      margin-top: 1rem;
      margin-bottom: 0;
    }

    &-header {
      padding: 0 1rem 0 0;
      background: none;
      box-shadow: none;

      h1 {
        display: none;
      }

      .book-overview-close-icon {
        position: absolute;
        right: 1rem;
        top: 4.25rem;
      }
    }

    &-sidebar {
      top: 0;
      left: 0;
      right: 0;
      bottom: auto;
      padding: 3.5rem 1rem 0.5rem;
      width: 100%;
      height: 7rem;
      flex-direction: row;
      align-items: center;
    }

    &-nav {
      margin: 0;

      li {
        padding: 0;
      }
    }

    .book-metadata {
      margin-right: 0.5rem;
    }

    &-title {
      padding: 0;
    }

    .book-cover {
      display: none;
    }

    &-nav {
      flex-direction: row;
    }

    &.book-overview-scrolled {
      .book-overview-header, .book-overview-sidebar {
        top: -3rem;
      }
    }
  }

  @media (max-width: 640px) {
    padding-top: 8.5rem;

    &-header .book-overview-close-icon {
      top: 3.625rem;
    }

    &-sidebar {
      flex-direction: column;
      height: 8.5rem;
    }

    .book-metadata {
      display: flex;
      align-items: baseline;
      margin-bottom: 0.5rem;
    }

    &-title {
      margin-right: 0.5rem;
    }
  }
}
</style>
