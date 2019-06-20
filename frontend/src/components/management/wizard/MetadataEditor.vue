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
  <TextField name="book-series" placeholder="Series" v-model="series">
    <template slot="icon">
    <GridIcon></GridIcon>
    </template>
  </TextField>
  <TextField
    type="number"
    name="book-series-order"
    placeholder="Order in Series"
    :inputConfig="{min: 1}"
    v-model="orderInSeries"
  >
    <template slot="icon">
    <HashIcon></HashIcon>
    </template>
  </TextField>
  <div class="button-bar">
    <Button slim :to="{name: 'book-management'}" :disabled="updating">Back</Button>
    <Button slim @click="updateMetadata" :loading="updating" :disabled="updating">Next</Button>
  </div>
</div>
</template>

<script>
import Button from '@/components/Button.vue';
import TextField from '@/components/TextField.vue';
import { GridIcon, HashIcon, TypeIcon, UserIcon } from 'vue-feather-icons';

export default {
  name: 'MetadataEditor',
  components: { UserIcon, TypeIcon, TextField, Button, HashIcon, GridIcon },
  async mounted() {
    this.updating = true;
    const { id } = this.$route.params;
    if (!id) {
      this.$router.replace({ name: 'book-management' });
      return;
    }
    const { data: { title, author, series, orderInSeries } } = await this.$api.get(`/books/${id}`);

    this.bookId = id;
    this.title = title;
    this.author = author;
    this.series = series;
    this.orderInSeries = orderInSeries.toString();
    this.updating = false;
  },
  data() {
    return {
      bookId: '',
      title: '',
      author: '',
      series: '',
      orderInSeries: '1',
      updating: false,
    };
  },
  methods: {
    async updateMetadata() {
      this.updating = true;
      try {
        await this.$api.patch(
          `/books/${this.bookId}`,
          {
            title: this.title,
            author: this.author,
            series: this.series,
            orderInSeries: Number(this.orderInSeries),
          },
          {
            headers: {
              'Content-Type': 'application/json',
            },
          },
        );
        this.$router.push({ name: 'table-of-contents', params: { id: this.bookId } });
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
