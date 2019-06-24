<template>
<div class="book-chapters">
  <h1>Chapters</h1>
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
  padding: 1.5rem 1rem 1rem 2rem;

  h1 {
    margin: 0;
  }

  &-nav {
    margin: 1rem 0 0;
    padding: 0;
    list-style-type: none;
    border-radius: 3px;
    box-sizing: border-box;
    box-shadow: 0 0.75rem 1rem rgba(0, 0, 0, 0.1);
    border: 1px solid rgba(0, 0, 0, 0.05);

    li {
      a {
        display: block;
        padding: 0.5rem;
        background: white;
        border-bottom: 1px solid rgba(0, 0, 0, 0.05);

        &:hover, &:active, &:focus {
          background: rgba(0, 0, 0, 0.001);
        }
      }

      &:first-child a {
        border-top-left-radius: 3px;
        border-top-right-radius: 3px;
      }

      &:last-child a {
        border-bottom-left-radius: 3px;
        border-bottom-right-radius: 3px;
        border-bottom: none;
      }
    }
  }
}
</style>
