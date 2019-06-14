<template>
<div class="class-mapper">
  <h2>CSS Classes</h2>
  <div class="class-mapper-list">
    <Expandable v-for="cls in classes" :key="cls.name">
      <template slot="header">
      {{ cls.name }} ({{ cls.occurrences }} Occurrences)
      </template>
      <ClassPreview :cls="cls">
      </ClassPreview>
    </Expandable>
  </div>
  <div class="button-bar">
    <Button slim :to="{name: 'book-upload'}" :disabled="updating">Back</Button>
    <Button slim :loading="updating" :disabled="updating">Next</Button>
  </div>
</div>
</template>

<script>
import TableOfContentsEntry from '@/components/wizard/TableOfContentsEntry.vue';
import Button from '@/components/Button.vue';
import TextField from '@/components/TextField.vue';
import { TypeIcon, UserIcon } from 'vue-feather-icons';
import ClassPreview from '@/components/wizard/ClassPreview.vue';
import Expandable from '@/components/Expandable.vue';

export default {
  name: 'ClassMapper',
  components: { Expandable, ClassPreview, UserIcon, TypeIcon, TextField, Button, TableOfContentsEntry },
  mounted() {
    const { book } = this.$route.params;
    if (!book) {
      this.$router.replace({ name: 'book-upload' });
      return;
    }
    this.bookId = book.id;
    this.classes = book.classes;
  },
  data() {
    return {
      bookId: '',
      classes: [],
      updating: false,
    };
  },
  methods: {},
};
</script>

<style lang="scss">
.class-mapper {
  display: flex;
  flex-direction: column;
  align-items: stretch;

  &-list {
    overflow-y: auto;
    flex-grow: 1;
    max-height: 100%;
  }
}
</style>
