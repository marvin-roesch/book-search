<template>
<div class="book-chapters">
  <LoadingSpinner v-if="loading"></LoadingSpinner>
  <transition-group
    tag="ol"
    class="book-chapters-nav"
    name="fade-slide-up">
    <li
      :style="{'transition-delay': `${index * 10}ms`}"
      v-for="(chapter, index) in chapters" :key="chapter.id">
      <router-link :to="{ 'name': 'chapter', params: { id: chapter.id } }">
        {{ chapter.title }}
      </router-link>
    </li>
  </transition-group>
</div>
</template>

<script>
import LoadingSpinner from '@/components/search/LoadingSpinner.vue';

export default {
  name: 'book-chapters',
  components: { LoadingSpinner },
  props: {
    book: Object,
  },
  data() {
    return {
      chapters: [],
      loading: false,
    };
  },
  async mounted() {
    this.loading = true;
    try {
      const { data: chapters } = await this.$api.get(`/books/${this.$route.params.id}/chapters`);
      this.chapters = chapters;
      document.title = `${this.book.title} - Chapters · Book Search`;
    } catch (error) {
      this.$handleApiError(error);
    }
    this.loading = false;
  },
};
</script>

<style lang="scss">
.book-chapters {
  box-sizing: border-box;
  position: relative;
  z-index: 0;

  .progress-spinner {
    margin-top: 1rem;
  }

  &-nav {
    margin: 0;
    padding: 1rem 0 0;
    list-style-type: none;
    border-radius: 3px;
    box-sizing: border-box;

    @media (max-width: $max-content-width) {
      padding-top: 0.125rem;
    }

    li {
      a {
        display: block;
        padding: 0.5rem 0.5rem 0.5rem 2rem;
        border-bottom: 1px solid rgba(0, 0, 0, 0.05);

        &:hover, &:active, &:focus {
          background: rgba(0, 0, 0, 0.01);
        }

        @media (max-width: $max-content-width) {
          padding-left: 1rem;
          padding-right: 1rem;
        }
      }

      &:last-child a {
        border-bottom: none;
      }
    }
  }
}
</style>
