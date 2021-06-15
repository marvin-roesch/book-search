# Book Search Deployment

This is a prepared [Docker Compose](https://docs.docker.com/compose/) file which allows for simple creation and execution of a Book Search instance. The following instructions will guide you through the setup process.

## OS-specific steps

### Windows

We recommend using the WSL2 backend for Docker on Windows. Follow [Docker's detaield guide](https://docs.docker.com/docker-for-windows/wsl/) in order to install it.

### MacOS

You need to have [Docker for Mac](https://docs.docker.com/docker-for-mac/install/) installed in order to run the application. Simply follow the instructions at the provided link.

### Linux

If you don't have Docker installed already, follow [the instructions](https://docs.docker.com/engine/install/) for your Distro and install the appropriate package. If the package does not come with Docker Compose pre-installed, consult [this guide](https://docs.docker.com/compose/install/) under the "Linux" section to set it up.

## Common steps

### Settings

There are a few settings you can tweak in the [Docker Compose file](docker-compose.yml) *before* starting the application. They are mostly concerned with security measures, so if your instance won't be publicly accessible in some capacity, you do not *need* to adjust them. All settings are controlled via environment variables in the Compose file.

| Setting            | Meaning |
| ------------------ | ------- |
| `DEFAULT_PASSWORD` | This is the password for the default `admin` user that gets created upon first start of the application. |
| `CRYPTO_KEY`       | This is a 128-character hexadecimal "password" which will be used to uniquely scramble the stored passwords for this instance in order to make reverse engineering them harder. |
| `DB_USERNAME`/`DB_DATA_USER` | The username for the database user which will handle the regular data of the application. By default, it will be set up with limited permissions so as to reduce a potential attack surface. |
| `DB_PASSWORD`/`DB_DATA_PASSWORD` | The password for the above user. |
| `DB_MIGRATION_USERNAME`/`DB_MIGRATION_USER` | The username for the database user which will perform "migrations" of the database schema. By default, it will have ultimate permissions of the database and should thus not be used in a way that's exposed to users. Migrations are changes to the database that can happen automatically across versions of the application. |
| `DB_MIGRATION_PASSWORD`/`DB_MIGRATION_PASSWORD` | The password for the above user. |
| `POSTGRES_PASSWORD` | The password for the default `postgres` database admin user. |

### Execution

In order to run the application, you must have a command line open in the directory of the `docker-compose.yml` file. Once you have reached this point, simply run `docker-compose up` in order to first start the application. Do not close the command line if you run the application this way or it will stop!

Once you've made certain that everything runs correctly, you may run the application using `docker-compose up -d`, whereupon you may close the command line again.

On Windows and macOS, you may also start all components through the Docker Desktop application after first execution.

### Usage

After you've started the application and everything is running, you should be able to reach your Book Search at [http://localhost:4080](http://localhost:4080).
