<template>
<div class="chapter-overlay" ref="container">
  <div class="chapter-overlay-header">
    <div class="chapter-overlay-title" v-if="chapter !== null">
      <h2>{{ book.title }} - {{ chapter.title }}</h2>
      <Share2Icon
        class="chapter-overlay-header-share-icon"
        :width="20"
        :height="20"
        @click.prevent.stop="share">
      </Share2Icon>
      <XIcon class="chapter-overlay-header-close-icon" @click.prevent.stop="close"></XIcon>
    </div>
  </div>
  <div class="chapter-overlay-content-container" @click.self.stop="close">
    <LoadingSpinner v-if="!contentLoaded"></LoadingSpinner>
    <Card class="chapter-overlay-content" v-else>
      <BookText :content="content" ref="text"></BookText>
    </Card>
  </div>
</div>
</template>

<script>
import Card from '@/components/Card.vue';
import { Share2Icon, XIcon } from 'vue-feather-icons';
import BookText from '@/components/BookText.vue';
import LoadingSpinner from '@/components/search/LoadingSpinner.vue';

export default {
  name: 'chapter-overlay',
  components: { Share2Icon, BookText, XIcon, Card, LoadingSpinner },
  data() {
    return {
      book: null,
      chapter: null,
      content: '',
      contentLoaded: false,
    };
  },
  async mounted() {
    try {
      const { data: { book, chapter, content } } = await this.$api.post(
        `/books/chapters/${this.$route.params.id}/search`,
        {
          query: this.$route.query.q,
          seriesFilter: null,
          bookFilter: null,
        },
      );

      this.book = book;
      this.chapter = chapter;
      this.content = content;
      this.contentLoaded = true;

      const { position } = this.$route.query;
      if (position !== undefined) {
        this.$nextTick(() => {
          const paragraphs = this.$refs.text.$el.querySelectorAll('p');

          if (position >= paragraphs.length) {
            return;
          }

          const paragraph = paragraphs[position];
          paragraph.classList.add('result-paragraph');
          this.$refs.container.scrollTop = paragraph.offsetTop - 70;
        });
      }
    } catch (error) {
      this.$handleApiError(error);
    }
  },
  methods: {
    close() {
      this.$router.back();
    },
    share() {
      const baseUrl = window.location.origin;
      const link = `${baseUrl}/chapters/${this.id}?q=${encodeURIComponent(this.query)}`;

      const el = document.createElement('textarea');
      el.value = link;
      document.body.appendChild(el);
      el.select();

      document.execCommand('copy');

      document.body.removeChild(el);
      this.$notifications.success('A link to this chapter has been copied to your clipboard!');
    },
  },
  beforeRouteEnter(to, from, next) {
    next();
  },
  beforeRouteLeave(to, from, next) {
    document.body.classList.remove('chapter-preview');
    next();
  },
};
</script>

<style scoped lang="scss">
.chapter-overlay {
  position: fixed;
  box-sizing: border-box;
  top: 0;
  bottom: 0;
  left: 0;
  right: 0;
  background: white;
  z-index: 2000;
  overflow-y: scroll;
  --delay: 0ms;
  cursor: pointer;

  &.fade-slide-up-enter-active, &.fade-slide-up-leave-active {
    overflow-y: hidden;
  }

  &-header {
    box-sizing: border-box;
    width: 100%;
    border-bottom: 1px solid rgba(0, 0, 0, 0.1);
    box-shadow: 0 0 1rem rgba(0, 0, 0, 0.1);
    position: sticky;
    top: 0;
    left: 0;
    background: white;
    display: flex;
    align-items: center;
    z-index: 2000;
    cursor: initial;
    padding: 0 1rem;

    .chapter-overlay-title {
      box-sizing: border-box;
      position: relative;
      margin: 0 auto;
      padding: 1rem 0;
      font-size: 1.25rem;
      z-index: 1001;
      width: 50%;
      max-width: $max-content-width;
      display: flex;
      align-items: center;

      @media (max-width: 1200px) {
        width: 70%;
      }

      @media (max-width: $max-content-width) {
        width: 100%;
      }

      h2 {
        font-size: 1.25rem;
        margin: 0;
        padding: 0;
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
    width: 50%;
    max-width: $max-content-width;
    margin: 0 auto;
    cursor: initial;

    @media (max-width: 1200px) {
      width: 70%;
    }

    @media (max-width: $max-content-width) {
      width: 100%;
    }

    &-container {
      padding: 1rem;
    }
  }
}
</style>
