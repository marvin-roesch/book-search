<template>
<Card class="user-list">
  <template slot="title">
  <router-link :to="{name: 'book-management'}" v-if="hasPermission('books.manage')">
    Books
  </router-link>
  <span v-if="hasPermission('books.manage')">&middot;</span>
  Users
  </template>
  <div class="user-table">
    <div class="user-table-header">Username</div>
    <div class="user-table-header">Permissions</div>
    <div class="user-table-header">Actions</div>
    <template v-for="user in users">
    <div class="user-table-cell" :key="`${user.id}-name`">
      {{ user.username }}
    </div>
    <div class="user-table-cell" :key="`${user.id}-permissions`">
      <CheckBox
        :name="`${user.id}-manage-books`"
        :value="user.canManageBooks"
        @input="updateBookPermissions(user, $event.target.checked)">
        Manage books
      </CheckBox>
      <CheckBox
        :name="`${user.id}-manage-users`"
        :value="user.canManageUsers"
        @input="updateUserPermissions(user, $event.target.checked)">
        Manage users
      </CheckBox>
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
    <div>
      <CheckBox
        :name="`new-user-manage-books`"
        :value="newCanManageBooks"
        @input="newCanManageBooks = $event.target.checked">
        Can manage books
      </CheckBox>
      <CheckBox
        :name="`new-user-manage-users`"
        :value="newCanManageUsers"
        @input="newCanManageUsers = $event.target.checked">
        Can manage users
      </CheckBox>
    </div>
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
import Card from '@/components/Card.vue';
import CheckBox from '@/components/CheckBox.vue';
import TextField from '@/components/TextField.vue';
import Button from '@/components/Button.vue';

export default {
  name: 'UserList',
  components: { Button, UserIcon, LockIcon, TextField, CheckBox, Card },
  data() {
    return {
      users: [],
      newUsername: '',
      newPassword: '',
      newCanManageBooks: false,
      newCanManageUsers: false,
      creating: false,
    };
  },
  computed: { ...mapState('auth', ['identity']), ...mapGetters('auth', ['hasPermission']) },
  async mounted() {
    try {
      const { data: users } = await this.$api.get('/auth/users');
      this.users = users;
    } catch (error) {
      this.$handleApiError(error);
    }
  },
  methods: {
    async updateBookPermissions(user, canManageBooks) {
      try {
        const { data: { message } } = await this.$api.patch(
          `/auth/users/${user.id}`,
          {
            canManageBooks,
            canManageUsers: user.canManageUsers,
          },
        );
        user.canManageBooks = canManageBooks;
        this.$notifications.success(message);
      } catch (error) {
        this.$handleApiError(error);
      }
    },
    async updateUserPermissions(user, canManageUsers) {
      try {
        const { data: { message } } = await this.$api.patch(
          `/auth/users/${user.id}`,
          {
            canManageBooks: user.canManageBooks,
            canManageUsers,
          },
        );
        user.canManageUsers = canManageUsers;
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
          `/auth/users`,
          {
            username: this.newUsername,
            password: this.newPassword,
            canManageBooks: this.newCanManageBooks,
            canManageUsers: this.newCanManageUsers,
          },
        );
        this.users.push(user);
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
    grid-template-columns: 1fr auto auto;
    overflow-y: auto;
    max-height: 100%;
    border-bottom: 1px solid rgba(0, 0, 0, 0.2);
    position: relative;

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
