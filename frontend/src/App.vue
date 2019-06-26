<template>
<div id="app">
  <transition name="fade">
    <keep-alive include="library">
      <router-view/>
    </keep-alive>
  </transition>
  <div class="notification-container" v-if="notifications !== null">
    <transition-group name="notification-slide" tag="div" class="notification-group">
      <Notification
        v-for="notification in notifications"
        :type="notification.type"
        :key="notification.id"
      >
        {{ notification.message }}
      </Notification>
    </transition-group>
  </div>
</div>
</template>

<script>
import Notification from '@/components/Notification.vue';
import { mapState } from 'vuex';

export default {
  name: 'app',
  components: { Notification },
  computed: mapState('notifications', {
    notifications: 'activeNotifications',
  }),
};
</script>

<style lang="scss">
html, body {
  margin: 0;
  padding: 0;
  width: 100%;
  height: 100%;
  background: #fafafa;
}

#app {
  font-family: 'Nunito Sans', Helvetica, Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  color: $base-text-color;
  display: flex;
  flex-direction: column;
  align-items: center;
  width: 100%;
  height: 100%;
}

.notification-container {
  width: 25vw;
  position: fixed;
  top: 0;
  left: 50%;
  margin-left: -12.5vw;
  z-index: 4000;

  @media (max-width: 1200px) {
    width: 40vw;
    margin-left: -20vw;
  }

  @media (max-width: 640px) {
    width: 80vw;
    margin-left: -40vw;
  }
}

.notification-group {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: stretch;

  .notification {
    z-index: 4001;
  }
}

.notification-slide {
  &-leave-active {
    position: absolute;
    left: 0;
    right: 0;
    z-index: 4000;
  }

  &-enter, &-leave-to {
    transform: translateY(-100%);
  }
}
</style>
