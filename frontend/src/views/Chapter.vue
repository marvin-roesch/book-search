<template>
<div class="chapter">
  <div :class="{
    'chapter-header': true,
    'chapter-header-hidden': scrolledDown
  }">
    <UserPanel></UserPanel>
    <div class="chapter-title">
      <h2>{{ bookTitle }} - {{ title }}</h2>
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
import Card from '@/components/Card.vue';
import BookText from '@/components/BookText.vue';
import LoadingSpinner from '@/components/search/LoadingSpinner.vue';
import UserPanel from '@/components/UserPanel.vue';
import { scrollAware } from '@/custom-directives';

export default {
  name: 'chapter',
  mixins: [scrollAware],
  components: { UserPanel, BookText, Card, LoadingSpinner },
  data() {
    return {
      bookTitle: '',
      title: '',
      content: '',
      contentLoaded: false,
    };
  },
  async mounted() {
    const query = this.$route.query.q;
    const { id } = this.$route.params;
    try {
      const { data: { book, chapter, content } } = await this.$api.post(
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
      this.$handleApiError(error);
    }
  },
};
</script>

<style scoped lang="scss">
.chapter {
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  align-items: center;
  width: 100%;
  height: 100%;

  &-header {
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
    padding: 0 1rem;
    transition: 0.2s top ease-out;

    @media (max-width: 1200px) {
      flex-direction: column;
      align-items: flex-start;
    }

    @media (max-height: 960px) {
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
      }

      @media (max-width: 960px) {
        left: 0;
        padding-left: 0;
      }
    }

    .chapter-title {
      box-sizing: border-box;
      position: relative;
      margin: 0 auto;
      padding: 1.5rem 0;
      font-size: 1.25rem;
      z-index: 1001;
      width: 50%;
      display: flex;
      align-items: center;

      @media (max-width: 1200px) {
        width: 70%;
        padding-top: 0;
      }

      @media (max-width: 960px) {
        width: 100%;
        padding-bottom: 1rem;
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
      padding: 6rem 1rem 1rem;

      @media (max-width: 1200px) {
        padding-top: 8rem;
      }
    }
  }
}
</style>
