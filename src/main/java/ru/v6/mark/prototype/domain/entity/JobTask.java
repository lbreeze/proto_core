package ru.v6.mark.prototype.domain.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import ru.v6.mark.prototype.domain.constant.Frequency;
import ru.v6.mark.prototype.domain.constant.HasProtocol;
import ru.v6.mark.prototype.domain.constant.JobType;
import ru.v6.mark.prototype.domain.constant.ProtocolAttribute;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name = "job_task")
@HasProtocol
public class JobTask extends BaseEntity {

    @Id
    @SequenceGenerator(name = "JOB_TASK_SEQ", sequenceName = "job_task_id_seq", initialValue = 1, allocationSize = 1)
    @GeneratedValue(generator = "JOB_TASK_SEQ", strategy = GenerationType.SEQUENCE)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "job_type")
    private JobType jobType;

    @Enumerated(EnumType.STRING)
    private Frequency frequency;

    @Column
    @Temporal(TemporalType.TIMESTAMP)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm", timezone = "Europe/Moscow")
    private Date initial;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @ProtocolAttribute("Интервал запуска")
    public Frequency getFrequency() {
        return frequency;
    }

    public void setFrequency(Frequency frequency) {
        this.frequency = frequency;
    }

    @ProtocolAttribute("Первый запуск")
    public Date getInitial() {
        return initial;
    }

    public void setInitial(Date initial) {
        this.initial = initial;
    }

    public JobType getJobType() {
        return jobType;
    }

    public void setJobType(JobType jobType) {
        this.jobType = jobType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        JobTask entity = (JobTask) o;

        if (!id.equals(entity.id)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "Задача \'" + jobType.getDescription() + "\'";
    }
}
