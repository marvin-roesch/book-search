<template>
<div class="chapter-citations">
  <h2>
    Assign the reference parameter to each chapter
    <Button slim @click="fillSuggestions" :disabled="updating">Use suggestions for all</Button>
  </h2>
  <ul class="chapter-citations-list">
    <li v-for="chapter in chapters" :key="chapter.id">
      <strong>{{ chapter.title }}</strong>
      <TextField
        :name="`chapter-${chapter.id}-citation-parameter`"
        placeholder="Citation Parameter"
        v-model="chapter.citationParameter"
      >
      </TextField>
      <div class="chapter-citations-suggestion-container">
        <span class="chapter-citations-suggestion">
          Suggestion: {{ chapter.citationParameterSuggestion }}
        </span>
        <Button
          slim
          @click="chapter.citationParameter = chapter.citationParameterSuggestion"
          :disabled="updating"
        >
          Use
        </Button>
      </div>
    </li>
  </ul>
  <div class="button-bar">
    <Button slim @click="$router.back()" :disabled="updating">Back</Button>
    <Button slim @click="submit" :loading="updating" :disabled="updating">Next</Button>
  </div>
</div>
</template>

<script>
import Button from '@/components/Button.vue';
import TextField from '@/components/TextField.vue';

export default {
  name: 'ChapterCitations',
  components: { TextField, Button },
  async mounted() {
    this.updating = true;
    const { id } = this.$route.params;
    if (!id) {
      this.$router.replace({ name: 'book-management' });
      return;
    }

    try {
      const { data: { chapters } } = await this.$api.get(`/books/${id}/chapter-citations`);

      this.bookId = id;
      this.chapters = chapters;
    } catch (error) {
      this.$handleApiError(error);
    }

    this.updating = false;
  },
  data() {
    return {
      bookId: '',
      chapters: [],
      updating: false,
    };
  },
  methods: {
    async submit() {
      this.updating = true;
      try {
        await this.$api.put(
          `/books/${this.bookId}/chapter-citations`,
          this.chapters.reduce((acc, { id, citationParameter }) => ({
            ...acc,
            [id]: (citationParameter || '').length === 0 ? null : citationParameter,
          }), {}),
          {
            headers: {
              'Content-Type': 'application/json',
            },
          },
        );
        this.$router.push({ name: 'book-classes', params: { id: this.bookId } });
      } catch (error) {
        this.$handleApiError(error);
        this.updating = false;
      }
    },
    fillSuggestions() {
      this.chapters.forEach((chapter) => {
        if (chapter.citationParameter === null || chapter.citationParameter.length === 0) {
          chapter.citationParameter = chapter.citationParameterSuggestion;
        }
      });
    },
  },
};
</script>

<style lang="scss">
.chapter-citations {
  display: flex;
  flex-direction: column;
  align-items: stretch;
  min-height: 0;

  h2 .button {
    height: auto;
    padding: 0.5rem;
    font-size: 0.9rem;
  }

  &-list {
    overflow-y: auto;
    flex-grow: 1;
    max-height: 100%;
    list-style-type: none;
    padding: 0;
    margin: 0;

    li {
      display: grid;
      grid-template-columns: 1fr 1fr 1fr;
      align-items: center;
      padding: 0.5rem 0.5rem;
      border-bottom: 1px solid rgba(0, 0, 0, 0.1);
      grid-gap: 0.5rem;
    }

    .chapter-citations-suggestion-container {
      display: flex;
      align-items: center;
      font-size: 0.9em;
      overflow: hidden;

      .chapter-citations-suggestion {
        text-overflow: ellipsis;
        min-width: 0;
        flex: 1;
        overflow: hidden;
        white-space: nowrap;
        margin-right: 0.25rem;
      }

      .button {
        font-size: 1em;
        height: auto;
        padding: 0.5rem;
        margin-left: auto;
      }
    }
  }
}
</style>
