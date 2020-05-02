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
    <BarChartIcon></BarChartIcon>
    </template>
  </TextField>
  <TextField
    name="book-citation-template"
    placeholder="Citation Template"
    v-model="citationTemplate"
  >
    <template slot="icon">
    <Link2Icon></Link2Icon>
    </template>
  </TextField>
  <TextField name="book-tags" placeholder="Tags" v-model="tags">
    <template slot="icon">
    <HashIcon></HashIcon>
    </template>
  </TextField>
  <CheckBox
    name="searched-by-default"
    :value="searchedByDefault"
    @input="searchedByDefault = $event.target.checked"
    class="metadata-editor__checkbox"
  >
    Include in search filter by default
  </CheckBox>
  <multiselect
    v-model="bookRoles"
    :options="roles"
    :multiple="true"
    placeholder="Everybody may access this book, add roles to restrict access"
    label="name"
    track-by="id"
    :close-on-select="false"
    :clear-on-select="false"
    :limit="5"
  >
  </multiselect>
  <div class="button-bar">
    <Button slim :to="{name: 'book-management'}" :disabled="updating">Back</Button>
    <div class="card-button-group">
      <Button slim @click="saveMetadata" :loading="updating" :disabled="updating">Save</Button>
      <Button slim @click="gotoNext" :loading="updating" :disabled="updating">Next</Button>
    </div>
  </div>
</div>
</template>

<script>
import Multiselect from 'vue-multiselect';
import { BarChartIcon, GridIcon, HashIcon, Link2Icon, TypeIcon, UserIcon } from 'vue-feather-icons';
import Button from '@/components/Button.vue';
import TextField from '@/components/TextField.vue';
import CheckBox from '@/components/CheckBox.vue';

export default {
  name: 'MetadataEditor',
  components: {
    CheckBox,
    UserIcon,
    TypeIcon,
    TextField,
    Button,
    HashIcon,
    GridIcon,
    BarChartIcon,
    Multiselect,
    Link2Icon,
  },
  async mounted() {
    this.updating = true;
    const { id } = this.$route.params;
    if (!id) {
      this.$router.replace({ name: 'book-management' });
      return;
    }

    try {
      const {
        data: {
          title, author, series, orderInSeries, citationTemplate, tags, searchedByDefault,
        },
      } = await this.$api.get(`/books/${id}`);

      const { data: roles } = await this.$api.get('/auth/roles');
      this.roles = roles;

      this.bookId = id;
      this.title = title;
      this.author = author;
      this.series = series;
      this.orderInSeries = orderInSeries.toString();
      this.citationTemplate = citationTemplate;
      this.tags = tags.join(', ');
      this.bookRoles = roles.filter(r => r.permissions.includes(`books.${id}.read`));
      this.searchedByDefault = searchedByDefault;
    } catch (error) {
      this.$handleApiError(error);
    }

    this.updating = false;
  },
  data() {
    return {
      bookId: '',
      title: '',
      author: '',
      series: '',
      orderInSeries: '1',
      citationTemplate: null,
      tags: '',
      bookRoles: [],
      updating: false,
      roles: [],
      searchedByDefault: true,
    };
  },
  methods: {
    async saveMetadata() {
      await this.updateMetadata({ name: 'book-management' });
      this.$notifications.success('Book metadata was successfully updated!');
    },
    async gotoNext() {
      await this.updateMetadata({ name: 'table-of-contents', params: { id: this.bookId } });
    },
    async updateMetadata(nextRoute) {
      this.updating = true;
      try {
        await this.$api.patch(
          `/books/${this.bookId}`,
          {
            title: this.title,
            author: this.author,
            series: this.series,
            orderInSeries: Number(this.orderInSeries),
            citationTemplate: (this.citationTemplate || '').length === 0 ? null : this.citationTemplate,
            tags: this.tags.split(',').map(tag => tag.trim()).filter(t => t.length > 0),
            permittedRoles: this.bookRoles.map(role => role.id),
            searchedByDefault: this.searchedByDefault,
          },
          {
            headers: {
              'Content-Type': 'application/json',
            },
          },
        );
        this.$router.push(nextRoute);
      } catch (error) {
        this.$handleApiError(error);
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
  min-height: 0;

  &-root {
    overflow-y: auto;
    flex-grow: 1;
    max-height: 100%;
  }

  &__checkbox {
    margin-top: 0.5rem;
  }

  .button-bar .card-button-group .button:first-child {
    margin-right: 0.5rem;
  }
}
</style>
