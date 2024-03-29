<template>
<div class="chapter-overlay" ref="container">
  <div class="chapter-overlay-header">
    <div class="chapter-overlay-title" v-if="chapter !== null">
      <h2>
        <router-link :to="{
          name: 'chapter',
          params: { id: chapter.id },
          query: { q: $route.query.q }
        }">
          {{ book.title }} - {{ chapter.title }}
        </router-link>
      </h2>
      <Share2Icon
        class="chapter-overlay-header-share-icon"
        :width="20"
        :height="20"
        @click.prevent.stop="share">
      </Share2Icon>
      <Button
        v-if="buildCitation() !== null"
        slim
        class="chapter-overlay-header-cite"
        @click="cite"
      >
        Cite
      </Button>
      <XIcon class="chapter-overlay-header-close-icon" @click.prevent.stop="close"></XIcon>
    </div>
  </div>
  <ChapterNavigation route="search-preview" fixed :prev="prevChapter" :next="nextChapter" />
  <div class="chapter-overlay-content-container" @click.self.stop="close">
    <ChapterNavigation route="search-preview" :prev="prevChapter" :next="nextChapter" />
    <LoadingSpinner v-if="!contentLoaded"></LoadingSpinner>
    <Card class="chapter-overlay-content" v-else>
      <BookText
        :book-title="book.title"
        :chapter-title="chapter.title"
        :content="content"
        :citation="buildCitation()"
        ref="text">
      </BookText>
    </Card>
    <ChapterNavigation route="search-preview" :prev="prevChapter" :next="nextChapter" />
  </div>
</div>
</template>

<script>
import { Share2Icon, XIcon } from 'vue-feather-icons';
import Card from '@/components/Card.vue';
import BookText from '@/components/BookText.vue';
import LoadingSpinner from '@/components/search/LoadingSpinner.vue';
import { buildCitation, copyText } from '@/utils';
import Button from '@/components/Button.vue';
import ChapterNavigation from '@/components/ChapterNavigation.vue';

export default {
  name: 'chapter-overlay',
  components: {
    Button,
    ChapterNavigation,
    Share2Icon,
    BookText,
    XIcon,
    Card,
    LoadingSpinner,
  },
  data() {
    return {
      book: null,
      chapter: null,
      content: '',
      contentLoaded: false,
      prevChapter: null,
      nextChapter: null,
    };
  },
  watch: {
    '$route.params.id': {
      async handler() {
        this.book = null;
        this.chapter = null;
        this.contentLoaded = false;

        try {
          const {
            data: {
              book, chapter, content, prevChapter, nextChapter,
            },
          } = await this.$api.post(
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
          this.prevChapter = prevChapter;
          this.nextChapter = nextChapter;
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
          } else {
            this.$nextTick(() => {
              this.$refs.container.scrollTop = 0;
            })
          }
        } catch (error) {
          this.$handleApiError(error);
        }
      },
      immediate: true,
    },
    book: {
      handler() {
        this.updateTitle();
      },
      immediate: true,
    },
    chapter: {
      handler() {
        this.updateTitle();
      },
      immediate: true,
    },
    '$route.query.q': {
      handler() {
        this.updateTitle();
      },
      immediate: true,
    },
  },
  methods: {
    close() {
      this.$router.replace({
        name: 'search',
        query: { ...this.$route.query, position: undefined },
      });
    },
    share() {
      const baseUrl = window.location.origin;
      const link = `${baseUrl}/chapters/${this.id}?q=${encodeURIComponent(this.$route.query.q)}`;
      copyText(link);
      this.$notifications.success('A link to this chapter has been copied to your clipboard!');
    },
    cite() {
      copyText(this.buildCitation());
      this.$notifications.success('A citation for this chapter has been copied to your clipboard!');
    },
    buildCitation() {
      return buildCitation(this.book, this.chapter);
    },
    updateTitle() {
      if (this.book === null || this.chapter === null) {
        return;
      }
      this.$nextTick(() => {
        document.title = `Results for '${this.$route.query.q}' in ${this.book.title} - ${this.chapter.title} · Book Search`;
      });
    },
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
  background: var(--body-bg);
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
    background: var(--section-bg);
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

    &-cite {
      margin-left: 0.5rem;
      height: auto;
      padding: 0.5rem;
      font-size: 0.9rem;
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
