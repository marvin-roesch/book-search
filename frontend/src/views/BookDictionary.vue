<template>
<div class="book-dictionary">
  <LoadingSpinner v-if="loading"></LoadingSpinner>
  <transition-group
    class="book-dictionary-entries"
    tag="div"
    name="fade-slide-up">
    <template>
    <div
      class="book-dictionary-entries-index book-dictionary-entries-header"
      v-if="dictionary.length > 0"
      :key="'index-header'">
      #
    </div>
    <div
      class="book-dictionary-entries-word book-dictionary-entries-header"
      v-if="dictionary.length > 0"
      :key="'word-header'">
      Term
    </div>
    <div
      class="book-dictionary-entries-occurrences book-dictionary-entries-header"
      v-if="dictionary.length > 0"
      :key="'occurrences-header'">
      Occurrences
    </div>
    </template>
    <template v-for="(term, index) in dictionary">
    <div
      class="book-dictionary-entries-index"
      :style="{'transition-delay': `${calculateDelay(index)}ms`}"
      :key="`${term.word}-index`">
      {{ index + 1 }}
    </div>
    <div
      class="book-dictionary-entries-word"
      :style="{'transition-delay': `${calculateDelay(index)}ms`}"
      :key="term.word">
      {{ term.word }}
    </div>
    <div
      class="book-dictionary-entries-occurrences"
      :style="{'transition-delay': `${calculateDelay(index)}ms`}"
      :key="`${term.word}-occurrences`">
      {{ term.occurrences }}
    </div>
    </template>
  </transition-group>
</div>
</template>

<script>
import LoadingSpinner from '@/components/search/LoadingSpinner.vue';

export default {
  name: 'book-dictionary',
  components: { LoadingSpinner },
  data() {
    return {
      dictionary: [],
      loading: false,
    };
  },
  async mounted() {
    this.loading = true;
    try {
      const { data: dictionary } = await this.$api.get(`/books/${this.$route.params.id}/dictionary`);
      this.dictionary = dictionary;
    } catch (error) {
      this.$handleApiError(error);
    }
    this.loading = false;
  },
  methods: {
    calculateDelay(index) {
      return Math.min(index * 10, 2000);
    },
  },
};
</script>

<style lang="scss">
.book-dictionary {
  box-sizing: border-box;
  padding: 1rem;

  &-entries {
    display: grid;
    grid-template-columns: min-content 1fr 1fr;

    &-header {
      font-weight: bold;
      border-bottom: 2px solid rgba(0, 0, 0, 0.2);
    }

    &-index {
      box-sizing: border-box;
      text-align: right;
      padding: 0.25rem 0.5rem;
      border-right: 1px solid rgba(0, 0, 0, 0.2);
    }

    &-word {
      box-sizing: border-box;
      padding: 0.25rem 0.5rem;
    }

    &-occurrences {
      box-sizing: border-box;
      padding: 0.25rem 0.5rem;
    }
  }
}
</style>
