<template>
<div class="chapter" ref="container">
  <div class="chapter-header">
    <div class="chapter-title">
      <h2>{{ bookTitle }} - {{ title }}</h2>
      <Share2Icon
        class="chapter-header-share-icon"
        :width="20"
        :height="20"
        @click.prevent.stop="share">
      </Share2Icon>
      <XIcon class="chapter-header-close-icon" @click.prevent.stop="close"></XIcon>
    </div>
  </div>
  <div class="chapter-content-container" @click.self.stop="close">
    <LoadingSpinner v-if="!contentLoaded"></LoadingSpinner>
    <Card class="chapter-content" v-else>
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
  props: {
    id: String,
    query: String,
    bookTitle: String,
    title: String,
    position: Number,
  },
  data() {
    return {
      content: '',
      contentLoaded: false,
    };
  },
  async mounted() {
    try {
      const { data: { content } } = await this.$api.post(
        `/books/chapters/${this.id}/search`,
        {
          query: this.query,
          seriesFilter: null,
          bookFilter: null,
        },
      );

      this.content = content;
      this.contentLoaded = true;

      if (this.position !== undefined) {
        this.$nextTick(() => {
          const paragraphs = this.$refs.text.$el.querySelectorAll('p');

          if (this.position >= paragraphs.length) {
            return;
          }

          const paragraph = paragraphs[this.position];
          this.$refs.container.scrollTop = paragraph.offsetTop - 70;
        });
      }
    } catch (error) {
      this.$handleApiError(error);
    }
  },
  methods: {
    close() {
      this.$emit('close');
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
};
</script>

<style scoped lang="scss">
.chapter {
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

  &-link {
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

    .chapter-title {
      box-sizing: border-box;
      position: relative;
      margin: 0 auto;
      padding: 1rem 0;
      font-size: 1.25rem;
      z-index: 1001;
      width: 50%;
      display: flex;
      align-items: center;

      @media (max-width: 1200px) {
        width: 70%;
      }

      @media (max-width: 960px) {
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
    margin: 0 auto;
    cursor: initial;

    @media (max-width: 1200px) {
      width: 70%;
    }

    @media (max-width: 960px) {
      width: 100%;
    }

    &-container {
      padding: 1rem;
    }
  }
}
</style>
