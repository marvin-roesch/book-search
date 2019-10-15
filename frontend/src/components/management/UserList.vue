<template>
<Card class="user-list">
  <template slot="title">
  <template v-if="hasPermission('books.manage')">
  <router-link :to="{name: 'book-management'}">Books</router-link>
  <span>&middot;</span>
  </template>
  <span>Users</span>
  <span>&middot;</span>
  <router-link :to="{name: 'role-management'}">Roles</router-link>
  </template>
  <div class="user-table">
    <div class="user-table-header">Username</div>
    <div class="user-table-header">Roles</div>
    <div class="user-table-header">Actions</div>
    <template v-for="user in users">
    <div class="user-table-cell" :key="`${user.id}-name`">
      {{ user.username }}
    </div>
    <div class="user-table-cell" :key="`${user.id}-roles`">
      <multiselect
        v-model="user.selectedRoles"
        :options="roles"
        :multiple="true"
        placeholder="Select roles"
        label="name"
        track-by="id"
        :close-on-select="false"
        :clear-on-select="false"
        :limit="5"
        @close="updateUser(user)"
      >
        <template slot="tag" slot-scope="{ option }">
        <span class="multiselect__tag multiselect__tag-no-icon" :key="option.id">
          <span>{{ option.name }}</span>
        </span>
        </template>
      </multiselect>
    </div>
    <div class="user-table-cell" :key="`${user.id}-actions`">
      <a href="#" @click.prevent="deleteUser(user.id)" v-if="user.id !== identity.id">Delete</a>
    </div>
    </template>
  </div>
  <form class="user-list-form">
    <h3>Create new user</h3>
    <TextField name="new-user-username" placeholder="Username" v-model="newUsername">
      <template slot="icon">
      <UserIcon></UserIcon>
      </template>
    </TextField>
    <TextField type="password" name="new-user-password" placeholder="Default password"
               v-model="newPassword">
      <template slot="icon">
      <LockIcon></LockIcon>
      </template>
    </TextField>
    <multiselect
      v-model="newRoles"
      :options="roles"
      :multiple="true"
      placeholder="Select roles"
      label="name"
      track-by="id"
      :close-on-select="false"
      :clear-on-select="false"
      :limit="5"
    >
    </multiselect>
    <div class="card-footer">
      <a href="#" @click.prevent="$router.back()">Back</a>
      <Button
        slim
        :disabled="creating || newUsername.length === 0 || newPassword.length === 0"
        @click="createUser"
      >
        Create
      </Button>
    </div>
  </form>
</Card>
</template>

<script>
import { mapGetters, mapState } from 'vuex';
import { LockIcon, UserIcon } from 'vue-feather-icons';
import Multiselect from 'vue-multiselect';
import Card from '@/components/Card.vue';
import TextField from '@/components/TextField.vue';
import Button from '@/components/Button.vue';

export default {
  name: 'UserList',
  components: { Button, UserIcon, LockIcon, TextField, Card, Multiselect },
  data() {
    return {
      users: [],
      roles: [],
      newUsername: '',
      newPassword: '',
      newRoles: [],
      creating: false,
    };
  },
  computed: { ...mapState('auth', ['identity']), ...mapGetters('auth', ['hasPermission']) },
  async mounted() {
    try {
      const { data: users } = await this.$api.get('/auth/users');
      const { data: roles } = await this.$api.get('/auth/roles');
      this.users = users.map(u => ({
        ...u,
        selectedRoles: roles.filter(r => u.roles.includes(r.id)),
      }));
      this.roles = roles;
    } catch (error) {
      this.$handleApiError(error);
    }
  },
  methods: {
    async updateUser(user) {
      try {
        const { data: { message } } = await this.$api.patch(
          `/auth/users/${user.id}`,
          {
            roles: user.selectedRoles.map(r => r.id),
          },
        );
        this.$notifications.success(message);
      } catch (error) {
        this.$handleApiError(error);
      }
    },
    async deleteUser(userId) {
      try {
        const { data: { message } } = await this.$api.delete(`/auth/users/${userId}`);
        this.users = this.users.filter(u => u.id !== userId);
        this.$notifications.success(message);
      } catch (error) {
        this.$handleApiError(error);
      }
    },
    async createUser() {
      this.creating = true;
      try {
        const { data: { message, user } } = await this.$api.put(
          '/auth/users',
          {
            username: this.newUsername,
            password: this.newPassword,
            initialRoles: this.newRoles.map(r => r.id),
          },
        );
        this.users.push({
          ...user,
          selectedRoles: this.roles.filter(r => user.roles.includes(r.id)),
        });
        this.$notifications.success(message);
      } catch (error) {
        this.$handleApiError(error);
      }
      this.creating = false;
    },
  },
};
</script>

<style scoped lang="scss">
.user-list {
  margin: 0 auto;
  width: 50vw;
  max-width: $max-content-width;
  max-height: 80vh;
  display: flex;
  flex-direction: column;
  align-items: stretch;
  box-sizing: border-box;

  h2 {
    display: flex;
    align-items: center;

    span {
      margin-left: 0.25rem;
    }

    a {
      margin-left: 0.25rem;
    }
  }

  .user-table {
    padding: 0;
    margin: 0;
    width: 100%;
    border-collapse: collapse;
    border-spacing: 0;
    display: grid;
    grid-template-columns: auto 1fr auto;
    overflow-y: auto;
    max-height: 100%;
    border-bottom: 1px solid rgba(0, 0, 0, 0.2);
    position: relative;
    align-items: center;

    &-header, &-cell {
      padding: 0.5rem;
    }

    &-header {
      position: sticky;
      font-weight: bold;
      border-bottom: 1px solid rgba(0, 0, 0, 0.2);
      top: 0;
      background: var(--section-bg);
      z-index: 1000;
    }

    &-cell {
      &:first-child {
        width: 100%;
      }

      &:nth-child(2), &:last-child {
        white-space: nowrap
      }

      a {
        margin-right: 0.5rem;

        &:last-child {
          margin-right: 0;
        }
      }
    }

    .checkbox {
      display: inline-block;
      margin-right: 0.5rem;

      &:last-child {
        margin-right: 0;
      }
    }
  }

  &-form {
    padding-top: 1rem;
    display: flex;
    flex-direction: column;
    align-items: stretch;

    h3 {
      padding: 0;
      margin: 0;
    }

    .checkbox {
      display: inline-block;
      margin-top: 0.5rem;
      margin-right: 0.5rem;

      &:last-child {
        margin-right: 0;
      }
    }

    .button {
      margin-top: 0.5rem;
      align-self: flex-end;
    }
  }

  @media (max-width: $max-content-width) {
    width: 100%;
  }
}
</style>
