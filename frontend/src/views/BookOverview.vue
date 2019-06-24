<template>
<div class="book-overview">
  <div class="book-overview-sidebar">
    <UserPanel></UserPanel>
    <div class="book-cover" ref="cover">
      <div style="display: none" ref="coverDummy"></div>
      <img :src="`/api/books/${bookId}/cover`" v-show="!hasOldCover && hasCover">
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
  <router-view>

  </router-view>
</div>
</template>

<script>
import UserPanel from '@/components/UserPanel.vue';
import { BookIcon, ListIcon } from 'vue-feather-icons';

export default {
  name: 'book-overview',
  components: { BookIcon, ListIcon, UserPanel },
  data() {
    return {
      bookId: '',
      book: null,
      chapters: [],
      hasCover: false,
      hasOldCover: false,
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
      const cover = document.getElementById(`book-${to.params.id}-cover`);
      if (cover !== null) {
        const bounds = vm.$refs.cover.getBoundingClientRect();
        const oldBounds = cover.getBoundingClientRect();
        const aspect = oldBounds.height / oldBounds.width;
        cover.style.top = `${bounds.top}px`;
        cover.style.left = `${bounds.left}px`;
        if (bounds.width > oldBounds.width) {
          cover.style.width = `${bounds.width}px`;
          cover.style.height = `${aspect * bounds.width}px`;
          cover.style.maxHeight = cover.style.height;
        }
        vm.$refs.coverDummy.style.width = cover.style.width;
        vm.$refs.coverDummy.style.height = cover.style.height;
        vm.$refs.coverDummy.style.display = 'block';
        cover.transitionListener = () => {
          vm.hasOldCover = false;
          vm.$refs.coverDummy.style.display = 'none';
          cover.style.display = 'none';
          cover.removeEventListener('transitionend', cover.transitionListener);
          cover.transitionListener = undefined;
        };
        cover.addEventListener('transitionend', cover.transitionListener);
      }
      vm.hasOldCover = cover !== null;
    });
  },
  beforeRouteLeave(to, from, next) {
    const cover = document.getElementById(`book-${this.bookId}-cover`);
    if (cover !== null) {
      cover.removeEventListener('transitionend', cover.transitionListener);
      this.hasCover = false;
      const bounds = this.$refs.cover.getBoundingClientRect();
      cover.style.top = `${bounds.top}px`;
      cover.style.left = `${bounds.left}px`;
      cover.style.display = '';
    }
    next();
  },
};
</script>

<style lang="scss">
.book-overview {
  box-sizing: border-box;
  padding: 0.5rem 2rem 2rem;
  width: 100%;

  &-sidebar {
    box-sizing: border-box;
    border-bottom: 1px solid rgba(0, 0, 0, 0.1);
    box-shadow: 0 0 1rem rgba(0, 0, 0, 0.1);
    position: fixed;
    top: 0;
    left: 0;
    bottom: 0;
    padding: 0.5rem 2rem 0;
  }

  .book-metadata {
    transition-delay: 0.1s;
  }

  &-nav {
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
  }

  &-title {
    box-sizing: border-box;
    padding: 0.25rem 0 0;
    font-weight: bold;
  }

  &-author {
    font-size: 0.8rem;
    color: lighten($base-text-color, 10%);
  }

  .book-cover {
    min-height: 20rem;
    width: 320rem;
    max-width: 20rem;
    margin-top: 1rem;

    img {
      max-width: 100%;
      max-height: 100%;
      display: block;
    }
  }
}
</style>
