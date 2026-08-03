CREATE TABLE recurring_transaction_executions (
                                                  id BIGSERIAL PRIMARY KEY,

                                                  recurring_transaction_id BIGINT NOT NULL,

                                                  scheduled_date DATE NOT NULL,

                                                  status VARCHAR(20) NOT NULL,

                                                  generated_transaction_id BIGINT,

                                                  error_message VARCHAR(500),

                                                  created_at TIMESTAMP WITH TIME ZONE NOT NULL,

                                                  completed_at TIMESTAMP WITH TIME ZONE,

                                                  CONSTRAINT fk_recurring_execution_recurring_transaction
                                                      FOREIGN KEY (recurring_transaction_id)
                                                          REFERENCES recurring_transactions (id)
                                                          ON DELETE RESTRICT,

                                                  CONSTRAINT fk_recurring_execution_generated_transaction
                                                      FOREIGN KEY (generated_transaction_id)
                                                          REFERENCES transactions (id)
                                                          ON DELETE SET NULL,

                                                  CONSTRAINT chk_recurring_execution_status
                                                      CHECK (
                                                          status IN (
                                                                     'PROCESSING',
                                                                     'SUCCESS',
                                                                     'FAILED'
                                                              )
                                                          ),

                                                  CONSTRAINT uk_recurring_execution_period
                                                      UNIQUE (
                                                              recurring_transaction_id,
                                                              scheduled_date
                                                          )
);

CREATE INDEX idx_recurring_executions_recurring
    ON recurring_transaction_executions (
                                         recurring_transaction_id,
                                         scheduled_date DESC
        );

CREATE INDEX idx_recurring_executions_status
    ON recurring_transaction_executions (
                                         status,
                                         created_at
        );