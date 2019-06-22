<template>
<div class="fullscreen">
  <UserPanel v-if="userPanel"></UserPanel>
  <slot></slot>
</div>
</template>

<script>
import UserPanel from '@/components/UserPanel.vue';

export default {
  name: 'fullscreen',
  components: { UserPanel },
  props: {
    userPanel: Boolean,
  },
};
</script>

<style lang="scss">
.fullscreen {
  display: flex;
  align-items: center;
  width: 100%;
  height: 100%;
  box-sizing: border-box;

  .user-panel {
    position: absolute;
    top: 0.5rem;
    left: 32px;
  }

  @media (max-width: 960px) {
    flex-direction: column;
    justify-content: flex-start;
    align-items: stretch;
    padding: 0 1rem;

    .user-panel {
      position: relative;
      top: 0;
      left: 0;
      padding-left: 0;
      padding-top: 0;
    }
  }
}
</style>
