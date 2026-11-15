package com.ar2723.stockgraph.collector;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BatchJobRunRepository extends JpaRepository<BatchJobRun, Long> {

	List<BatchJobRun> findTop20ByJobNameOrderByStartedAtDesc(String jobName);

}
