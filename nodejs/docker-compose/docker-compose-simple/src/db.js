let config = process.env;

export const knex = require('knex')({
    client: 'pg',
    connection: {
        connectionString: config.DATABASE_URL,
        host: config['DB_HOST'],
        port: config['DB_PORT'],
        user: config['DB_USER'],
        database: config['DB_NAME'],
        password: config['DB_PASSWORD'],
        ssl: config['DB_SSL'] ? { rejectUnauthorized: false } : false,
    },
});
