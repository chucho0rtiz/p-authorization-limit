drop table if exists authorizations;

drop table if exists customers;


create table customers (
    customer_id varchar(25) primary key,
    daily_limit bigint not null,
    consumed_amount bigint not null,
    available_amount bigint not null,
    role_admin boolean not null default false,
    state boolean not null default true
);

create table authorizations (
    id bigint generated always as identity primary key,
    transaction_id varchar(25) not null unique,
    amount bigint not null,
    customer_id varchar(25) not null,
    constraint fk_customers_authorizations foreign KEY (customer_id) references customers (customer_id) on delete RESTRICT
);


insert into
    customers (
    customer_id,
    daily_limit,
    consumed_amount,
    available_amount,
    role_admin,
    state
)
values
    ('CUS-001', 1000000, 250000, 750000, true, true),
    ('CUS-002', 2000000, 450000, 1550000, false, true),
    ('CUS-003', 500000, 100000, 400000, false, true),
    ('CUS-004', 3000000, 1250000, 1750000, true, true),
    ('CUS-005', 800000, 800000, 0, false, false),
    ('CUS-006', 1000000, 0, 1000000, false, true),
    ('CUS-007', 100000, 90000, 10000, false, true),
    ('CUS-INACTIVE', 500000, 0, 500000, false, false),
    ('CUS-ADMIN', 0, 0, 0, true, true);

insert into
    authorizations (transaction_id, amount, customer_id)
values
    ('TX-10001', 150000, 'CUS-001'),
    ('TX-10002', 200000, 'CUS-002'),
    ('TX-10003', 100000, 'CUS-003'),
    ('TX-10004', 750000, 'CUS-004'),
    ('TX-10005', 800000, 'CUS-005');