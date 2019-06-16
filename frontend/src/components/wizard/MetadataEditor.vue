<template>
<div class="metadata-editor">
  <h2>Book Information</h2>
  <TextField name="book-title" placeholder="Title" v-model="title">
    <template slot="icon">
    <TypeIcon></TypeIcon>
    </template>
  </TextField>
  <TextField name="book-author" placeholder="Author" v-model="author">
    <template slot="icon">
    <UserIcon></UserIcon>
    </template>
  </TextField>
  <div class="button-bar">
    <Button slim :to="{name: 'book-upload'}" :disabled="updating">Back</Button>
    <Button slim @click="updateMetadata" :loading="updating" :disabled="updating">Next</Button>
  </div>
</div>
</template>

<script>
import axios from 'axios';
import TableOfContentsEntry from '@/components/wizard/TableOfContentsEntry.vue';
import Button from '@/components/Button.vue';
import TextField from '@/components/TextField.vue';
import { TypeIcon, UserIcon } from 'vue-feather-icons';

export default {
  name: 'MetadataEditor',
  components: { UserIcon, TypeIcon, TextField, Button, TableOfContentsEntry },
  mounted() {
    const { book } = this.$route.params;
    if (!book) {
      this.$router.replace({ name: 'book-upload' });
      return;
    }
    this.bookId = book.id;
    this.title = book.title;
    this.author = book.author;
  },
  data() {
    return {
      bookId: '',
      title: '',
      author: '',
      updating: false,
    };
  },
  methods: {
    async updateMetadata() {
      this.updating = true;
      try {
        const { data: book } = await axios.patch(
          `/api/book/${this.bookId}`,
          {
            title: this.title,
            author: this.author,
          },
          {
            headers: {
              'Content-Type': 'application/json',
            },
          },
        );
        this.$router.push({ name: 'table-of-contents', params: { book } });
      } catch (error) {
        this.updating = false;
      }
    },
  },
};
</script>

<style lang="scss">
.metadata-editor {
  display: flex;
  flex-direction: column;
  align-items: stretch;

  &-root {
    overflow-y: auto;
    flex-grow: 1;
    max-height: 100%;
  }
}
</style>
