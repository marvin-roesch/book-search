<template>
<div class="chapter">
  <div :class="{
    'chapter-toolbar-container': true,
    'chapter-toolbar-container-hidden': scrolledDown
  }">
    <UserPanel></UserPanel>
    <div class="chapter-toolbar">
      <div class="chapter-toolbar-title">
        <h2>{{ bookTitle }} - {{ title }}</h2>
        <XIcon class="chapter-toolbar-close-icon" @click.prevent.stop="$router.back()"></XIcon>
      </div>
      <SearchBar toolbar :query="$route.query.q" @search="changeQuery"></SearchBar>
      <QuickHelp></QuickHelp>
    </div>
  </div>
  <div class="chapter-content-container">
    <LoadingSpinner v-if="!contentLoaded"></LoadingSpinner>
    <Card class="chapter-content" v-else>
      <BookText :content="content" ref="text"></BookText>
    </Card>
  </div>
</div>
</template>

<script>
import axios from 'axios';
import Card from '@/components/Card.vue';
import BookText from '@/components/BookText.vue';
import LoadingSpinner from '@/components/search/LoadingSpinner.vue';
import UserPanel from '@/components/UserPanel.vue';
import { scrollAware } from '@/custom-directives';
import SearchBar from '@/components/search/SearchBar.vue';
import { XIcon } from 'vue-feather-icons';
import QuickHelp from '@/components/search/QuickHelp.vue';

export default {
  name: 'chapter',
  mixins: [scrollAware],
  components: { QuickHelp, SearchBar, UserPanel, BookText, Card, XIcon, LoadingSpinner },
  data() {
    return {
      bookTitle: '',
      title: '',
      content: '',
      contentLoaded: false,
      cancelToken: null,
    };
  },
  async mounted() {
    await this.search();
  },
  methods: {
    changeQuery(query) {
      this.$router.replace({
        name: 'chapter',
        params: this.$route.params,
        query: {
          q: query,
        },
      });
    },
    async search() {
      if (this.cancelToken !== null) {
        this.cancelToken.cancel();
      }
      this.cancelToken = axios.CancelToken.source();

      const query = this.$route.query.q;
      const { id } = this.$route.params;
      try {
        const { data: { book, chapter, content } } = !query
          ? await this.$api.get(`/books/chapters/${id}`)
          : await this.$api.post(
            `/books/chapters/${id}/search`,
            {
              query,
              seriesFilter: null,
              bookFilter: null,
            },
          );

        this.bookTitle = book.title;
        this.title = chapter.title;
        this.content = content;
        this.contentLoaded = true;
      } catch (error) {
        this.contentLoaded = true;
        if (axios.isCancel(error)) {
          return;
        }
        this.$handleApiError(error);
      }
    },
  },
  watch: {
    '$route.query.q': function () {
      this.search();
    },
  },
};
</script>

<style lang="scss">
.chapter {
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  align-items: center;
  width: 100%;
  height: 100%;

  &-toolbar-container {
    box-sizing: border-box;
    width: 100%;
    border-bottom: 1px solid rgba(0, 0, 0, 0.1);
    box-shadow: 0 0 1rem rgba(0, 0, 0, 0.1);
    position: fixed;
    top: 0;
    left: 0;
    background: white;
    display: flex;
    align-items: center;
    z-index: 2000;
    cursor: initial;
    transition: 0.2s top ease-out;

    @media (max-width: 1200px) {
      flex-direction: column;
      align-items: flex-start;
    }

    @media (max-width: $max-content-width) and (max-height: $max-content-width) {
      &-hidden {
        box-shadow: none;
        top: -3rem;
      }
    }

    .user-panel {
      position: absolute;
      left: 2rem;
      top: 0.5rem;

      @media (max-width: 1200px) {
        position: relative;
        left: 0;
        top: 0;
        padding-bottom: 0.5rem;
      }

      @media (max-width: $max-content-width) {
        left: 0;
      }
    }

    .chapter-toolbar {
      box-sizing: border-box;
      position: relative;
      margin: 0 auto;
      padding: 1rem 1rem 0.5rem;
      z-index: 1001;
      width: 50%;
      max-width: $max-content-width;
      display: flex;
      align-items: stretch;
      flex-direction: column;

      @media (max-width: 1200px) {
        width: 70%;
        padding-top: 0;
      }

      @media (max-width: $max-content-width) {
        width: 100%;
      }

      @media (max-width: 640px) {
        .quick-help {
          display: none !important;
        }
      }

      h2 {
        font-size: 1.25rem;
        margin: 0;
        padding: 0;
      }

      &-title {
        display: flex;
        align-items: stretch;
        padding-top: 0.6rem;
        padding-bottom: 0.5rem;

        @media (max-width: 1200px) {
          padding-top: 0;
        }
      }

      &-close-icon {
        margin-left: auto;
        cursor: pointer;
      }
    }

    &-share-icon {
      margin-left: 0.5rem;
      cursor: pointer;
    }

    &-close-icon {
      margin-left: auto;
      cursor: pointer;
    }
  }

  &-content {
    margin: 0 auto;
    cursor: initial;

    &-container {
      box-sizing: border-box;
      width: 50%;
      max-width: $max-content-width;
      padding: 10rem 1rem 1rem;

      @media (max-width: 1200px) {
        padding-top: 13rem;
        width: 70%;
      }

      @media (max-width: $max-content-width) {
        width: 100%;
      }

      @media (max-width: 640px) {
        padding-top: 10rem;
      }
    }
  }
}
</style>
