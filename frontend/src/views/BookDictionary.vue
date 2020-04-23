<template>
<div class="book-dictionary">
  <LoadingSpinner v-if="loading"></LoadingSpinner>
  <transition-group
    class="book-dictionary-entries"
    tag="table"
    name="fade-slide-up"
  >
    <tr :key="'header'">
      <th
        class="book-dictionary-entries-index book-dictionary-entries-header"
        v-if="dictionary.length > 0"
      >
        #
      </th>
      <th
        class="book-dictionary-entries-word book-dictionary-entries-header"
        v-if="dictionary.length > 0"
      >
        Term
      </th>
      <th
        class="book-dictionary-entries-occurrences book-dictionary-entries-header"
        v-if="dictionary.length > 0"
      >
        Occurrences
      </th>
    </tr>
    <template v-for="(term, index) in dictionary">
    <tr :key="term.word">
      <td
        class="book-dictionary-entries-index"
        :style="{'transition-delay': `${calculateDelay(index)}ms`}"
      >
        {{ index + 1 }}
      </td>
      <td
        class="book-dictionary-entries-word"
        :style="{'transition-delay': `${calculateDelay(index)}ms`}"
      >
        <router-link
          :to="{
        name: 'search',
        query: { q: term.word, series: '', books: $route.params.id }
      }"
        >
          {{ term.word }}
        </router-link>
      </td>
      <td
        class="book-dictionary-entries-occurrences"
        :style="{'transition-delay': `${calculateDelay(index)}ms`}"
      >
        {{ term.occurrences }}
      </td>
    </tr>
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
    width: 100%;

    th {
      text-align: left;
    }

    &-header {
      font-weight: bold;
      border-bottom: 2px solid rgba(0, 0, 0, 0.2);
      white-space: nowrap
    }

    &-index {
      box-sizing: border-box;
      text-align: right;
      padding: 0.25rem 0.5rem;
      border-right: 1px solid rgba(0, 0, 0, 0.2);
      white-space: nowrap
    }

    &-word {
      box-sizing: border-box;
      padding: 0.25rem 0.5rem;
      width: 50%;
    }

    &-occurrences {
      box-sizing: border-box;
      padding: 0.25rem 0.5rem;
      width: 50%;
    }
  }
}
</style>
