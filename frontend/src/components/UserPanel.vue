<template>
<div class="user-panel" v-if="identity !== null">
  <div class="user-panel-menu">
    <div class="user-panel-menu-trigger" @click="menuVisible = !menuVisible" ref="trigger">
      <div class="user-panel-menu-trigger-icon">
        <UserIcon width="28" height="28"></UserIcon>
      </div>
      {{ identity.username }}
    </div>
    <transition name="fade">
      <ul
        class="user-panel-menu-content"
        v-closable="{
        exclude: ['trigger'],
        handler() { menuVisible = false; }
      }"
        v-if="menuVisible">
        <li>
          <router-link :to="{name: 'account'}">Account Settings</router-link>
        </li>
        <li><a href="#" @click.prevent="logout">Logout</a></li>
      </ul>
    </transition>
  </div>
  <ul class="user-panel-links">
    <li>
      <router-link :to="{name: 'home'}">Search</router-link>
    </li>
    <li>
      <router-link :to="{name: 'read'}">Library</router-link>
    </li>
    <li v-if="identity.canManageBooks">
      <router-link :to="{name: 'book-management'}">Manage</router-link>
    </li>
  </ul>
</div>
</template>

<script>
import { mapState } from 'vuex';
import { UserIcon } from 'vue-feather-icons';

export default {
  name: 'UserPanel',
  components: { UserIcon },
  data() {
    return {
      menuVisible: false,
    };
  },
  computed: mapState('auth', ['identity']),
  methods: {
    async logout() {
      try {
        await this.$store.dispatch('auth/logout');
        this.$router.replace({ name: 'login' });
      } catch (error) {
        this.$handleApiError(error);
      }
    },
  },
};
</script>

<style lang="scss">
.user-panel {
  display: flex;
  align-items: center;
  padding: 1rem 2rem 1rem 1rem;

  &-menu {
    box-sizing: border-box;
    position: relative;

    &-trigger {
      box-sizing: border-box;
      display: flex;
      align-items: center;
      cursor: pointer;

      &-icon {
        box-sizing: border-box;
        width: 32px;
        height: 32px;
        border-radius: 100%;
        border: 2px solid $base-text-color;
        position: relative;
        margin-right: 0.5rem;
      }

      &:hover {
        color: lighten($base-text-color, 10%);

        &-icon {
          border-color: lighten($base-text-color, 10%);
        }
      }
    }

    &-content {
      position: absolute;
      box-sizing: border-box;
      top: 100%;
      left: 0;
      margin-top: 0.6rem;
      padding: 0.5rem 0;
      background: white;
      border: 1px solid #C8C8C8;
      border-radius: 3px;
      box-shadow: 0 0.25rem 1rem rgba(0, 0, 0, 0.1);
      width: auto;
      list-style-type: none;
      display: flex;
      flex-direction: column;
      align-items: stretch;
      font-size: 0.9rem;
      z-index: 3000;

      li {
        position: relative;
        white-space: nowrap;

        a {
          display: block;
          padding: 0.25rem 0.5rem;
          color: $base-text-color;

          &:hover, &:focus, &:active {
            cursor: pointer;
            color: $base-text-color;
            background: rgba(0, 0, 0, 0.05);
          }
        }
      }

      &:before {
        position: absolute;
        box-sizing: border-box;
        background: white;
        content: '';
        width: 0.5rem;
        height: 0.5rem;
        top: -0.25rem;
        left: 0.7rem;
        margin-top: -1px;
        transform-origin: 50%;
        transform: rotate(45deg);
        border: 1px solid #C8C8C8;
        border-bottom: none;
        border-right: none;
      }
    }
  }

  &-links {
    list-style-type: none;
    padding: 0;
    margin: 0 0 0 0.75rem;
    display: flex;
    align-items: center;

    li {
      margin-right: 0.25rem;

      &:after {
        display: inline-block;
        content: '·';
        margin-left: 0.25rem;
      }

      &:last-child {
        margin-right: 0;

        &:after {
          display: none;
        }
      }
    }
  }
}
</style>
