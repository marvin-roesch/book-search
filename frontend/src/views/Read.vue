<template>
<div class="read-overview">
  <UserPanel></UserPanel>
  <SeriesTree :series="s" v-for="s in series" :key="s.name"></SeriesTree>
</div>
</template>

<script>
import SeriesTree from '@/components/read/SeriesTree.vue';
import UserPanel from '@/components/UserPanel.vue';

export default {
  name: 'read',
  components: { UserPanel, SeriesTree },
  data() {
    return {
      series: [],
    };
  },
  async mounted() {
    try {
      const { data: allSeries } = await this.$api.get('/books/series');
      this.series = allSeries;
    } catch (error) {
      this.$handleApiError(error);
    }
  },
};
</script>

<style lang="scss">
.read-overview {
  box-sizing: border-box;
  padding: 0.5rem 2rem 2rem;
  width: 100%;

  .user-panel {
  }
}
</style>
