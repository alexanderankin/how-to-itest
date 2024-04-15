terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "5.34.0"
    }
  }
}

provider "aws" {
  region = "us-east-1" # represent
}

variable "prefix" { default = "how-to-itest-ddb-sdk" }

resource "aws_vpc" "vpc" {
  cidr_block                       = "10.0.0.0/24"
  assign_generated_ipv6_cidr_block = true

  tags = { Name = "${var.prefix}-vpc" }
}

// make a public subnet and jump host
resource "aws_subnet" "public" {
  vpc_id                          = aws_vpc.vpc.id
  map_public_ip_on_launch         = false
  cidr_block                      = cidrsubnet(aws_vpc.vpc.cidr_block, 4, 0)
  ipv6_cidr_block                 = cidrsubnet(aws_vpc.vpc.ipv6_cidr_block, 8, 0)
  assign_ipv6_address_on_creation = true

  availability_zone = "us-east-1a" # just not us-east-1e, apparently

  tags = { Name = "${var.prefix}-jump-host" }
}

resource "aws_internet_gateway" "gw" {
  vpc_id = aws_vpc.vpc.id

  tags = { Name = "${var.prefix}-igw" }
}

resource "aws_route_table" "public" {
  vpc_id = aws_vpc.vpc.id

  tags = { Name = "${var.prefix}-public" }
}

resource "aws_route" "public" {
  route_table_id         = aws_route_table.public.id
  gateway_id             = aws_internet_gateway.gw.id
  destination_cidr_block = "0.0.0.0/0"
}

resource "aws_route" "public_v6" {
  route_table_id              = aws_route_table.public.id
  gateway_id                  = aws_internet_gateway.gw.id
  destination_ipv6_cidr_block = "::/0"
}

resource "aws_route_table_association" "public" {
  route_table_id = aws_route_table.public.id
  subnet_id      = aws_subnet.public.id
}

resource "aws_security_group" "jump-host" {
  name = "${var.prefix}-jump-host"
  tags = { Name = "${var.prefix}-very-permissive" }

  description = "Security group with only inbound SSH and all outbound traffic allowed"
  vpc_id      = aws_vpc.vpc.id

  // Allow all outbound traffic
  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"          // -1 means all protocols
    cidr_blocks = ["0.0.0.0/0"] # dest

    ipv6_cidr_blocks = ["::/0"] # dest
  }

  // Allow only inbound SSH traffic
  ingress {
    from_port   = 22
    to_port     = 22
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"] # source

    ipv6_cidr_blocks = ["::/0"] # source
  }

  // allow all ingress from vpc because of nat
  ingress {
    from_port        = 0
    to_port          = 0
    protocol         = "-1"
    cidr_blocks      = [aws_vpc.vpc.cidr_block]
    ipv6_cidr_blocks = [aws_vpc.vpc.ipv6_cidr_block]
  }
}

data "aws_ami" "ubuntu" {
  most_recent = true

  filter {
    name   = "name"
    values = ["ubuntu/images/hvm-ssd/ubuntu-jammy-22.04-arm64-server-*"]
  }

  filter {
    name   = "virtualization-type"
    values = ["hvm"]
  }

  #owners = ["099720109477"] # Canonical
  # https://stackoverflow.com/a/76841570
  owners = ["amazon"] # Canonical
}

resource "aws_instance" "jump_host" {
  # t4g.nano $3.15 (as of 2024/02/01)
  # associate_public_ip_address is $3.75 (as of 2024/02/01)

  ami                         = data.aws_ami.ubuntu.id
  instance_type               = "t4g.nano"
  vpc_security_group_ids      = [aws_security_group.jump-host.id]
  associate_public_ip_address = true
  ipv6_address_count          = 1

  subnet_id = aws_subnet.public.id

  user_data = <<-EOF
    #!/bin/bash
    f=/home/ubuntu/.ssh/authorized_keys; touch $f; chmod 600 $f;
    echo '${filebase64("~/.ssh/id_rsa.pub")}' | base64 -d >> $f
    curl -fSsL github.com/alexanderankin.keys >> $f

    apt update
    apt install curl unzip -y
    #curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip"
    curl "https://awscli.amazonaws.com/awscli-exe-linux-aarch64.zip" -o "awscliv2.zip"
    unzip -u awscliv2.zip
    ./aws/install

    token="$(curl -X PUT -H 'X-aws-ec2-metadata-token-ttl-seconds: 300' http://169.254.169.254/latest/api/token)"
    # instance_id="$(curl -H "X-aws-ec2-metadata-token: $token" http://169.254.169.254/latest/meta-data/instance-id)"
    # aws_region="$(curl -H "X-aws-ec2-metadata-token: $token" http://169.254.169.254/latest/meta-data/placement/region)"
    outbound_mac="$(curl -H "X-aws-ec2-metadata-token: $token" http://169.254.169.254/latest/meta-data/mac)"

    # amazon linux only
    # outbound_eni_id="$(curl -H "X-aws-ec2-metadata-token: $token" http://169.254.169.254/latest/meta-data/network/interfaces/macs/$outbound_mac/interface-id)"

    outbound_eni_name=$(for interface in $(ls -1 /sys/class/net) ; do echo $interface $(cat /sys/class/net/$interface/address) ; done | grep $outbound_mac | awk ' { print $1 } ')
    # debugging:
    # for interface in $(ls -1 /sys/class/net) ; do echo $interface $(cat /sys/class/net/$interface/address) ; done

    nat_interface=$(ip link show dev "$outbound_eni_name" | head -n 1 | awk '{print $2}' | sed s/://g )

    rp=/etc/sysctl.d/fnat-disable-rp_filter.conf

    cat > $rp <<CONF_EOF
    net.ipv4.conf.all.rp_filter = 0
    net.ipv4.conf.default.rp_filter = 0
    CONF_EOF

    ls -1 /sys/class/net/ | while read line ; do echo "net.ipv4.conf.$line.rp_filter = 0" >> $rp; done
    ls -1 /sys/class/net/ | while read line ; do echo "net.ipv4.conf.$line.rp_filter = 0" | sudo tee -a $rp; done

    fwd=/etc/sysctl.d/fnat-enable-forwarding.conf
    cat > $fwd <<CONF_EOF
    net.ipv4.ip_forward = 1
    net.ipv6.conf.all.forwarding = 1
    CONF_EOF

    # reload
    sysctl --system

    iptables -t nat -A POSTROUTING -o "$nat_interface" -j MASQUERADE -m comment --comment "NAT routing rule installed by fck-nat"
    # debugging:
    # iptables -vnL --line-numbers -t nat
  EOF

  source_dest_check = false # nat

  tags = { Name = "${var.prefix}-jump-host" }

  iam_instance_profile = aws_iam_instance_profile.jump_host_profile.id

  lifecycle {
    ignore_changes = [ami]
  }
}

resource "aws_iam_instance_profile" "jump_host_profile" {
  name = "${var.prefix}-jump-host-profile"
  tags = { Name = "${var.prefix}-jump-host-profile" }
  role = aws_iam_role.jump_host_role.name
}

resource "aws_iam_role" "jump_host_role" {
  name = "${var.prefix}-jump-host-ec2-assumable-role"
  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = "sts:AssumeRole"
        Principal = {
          Service = "ec2.amazonaws.com"
        }
      }
    ]
  })
}

resource "aws_iam_policy" "jump_host_ddb_prefix_all" {
  name = "${var.prefix}-dynamodb-full-access-policy"
  tags = {
    Name = "${var.prefix}-dynamodb-full-access-policy"
  }
  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect   = "Allow"
        Action   = "dynamodb:*"
        Resource = "arn:aws:dynamodb:*:*:table/${var.prefix}-*"
      }
    ]
  })
}

resource "aws_iam_role_policy_attachment" "jump_host_role_policies" {
  for_each = {
    # AmazonDynamoDBFullAccess = "arn:aws:iam::aws:policy/AmazonDynamoDBFullAccess"
    # AmazonDynamoDBReadOnlyAccess = "arn:aws:iam::aws:policy/AmazonDynamoDBReadOnlyAccess"
    "${var.prefix}-dynamodb-full-access-policy" = aws_iam_policy.jump_host_ddb_prefix_all.arn
  }

  role       = aws_iam_role.jump_host_role.name
  policy_arn = each.value
}

// make private subnet, asg
resource "aws_subnet" "application" {
  count = 2

  tags = { Name = "${var.prefix}-application-${count.index}" }

  vpc_id                          = aws_vpc.vpc.id
  map_public_ip_on_launch         = false
  assign_ipv6_address_on_creation = true
  cidr_block                      = cidrsubnet(aws_vpc.vpc.cidr_block, 4, 4 + count.index)
  ipv6_cidr_block                 = cidrsubnet(aws_vpc.vpc.ipv6_cidr_block, 8, 4 + count.index)
  availability_zone               = "us-east-1${["a", "b", "c", "d"][count.index % 4]}"
}

resource "aws_route_table" "nat" {
  vpc_id = aws_vpc.vpc.id
  route {
    cidr_block           = "0.0.0.0/0"
    network_interface_id = aws_instance.jump_host.primary_network_interface_id
  }
  route {
    ipv6_cidr_block      = "::/0"
    network_interface_id = aws_instance.jump_host.primary_network_interface_id
  }
}

resource "aws_route_table_association" "application" {
  for_each       = aws_subnet.application
  route_table_id = aws_route_table.nat.id # nat
  subnet_id      = each.value.id
}

resource "aws_security_group" "application" {
  vpc_id = aws_vpc.vpc.id
  name   = "${var.prefix}-application"
  tags   = { Name = "${var.prefix}-application" }

  egress {
    from_port        = 443
    to_port          = 443
    protocol         = "tcp"
    cidr_blocks      = [aws_vpc.vpc.cidr_block]
    ipv6_cidr_blocks = [aws_vpc.vpc.ipv6_cidr_block]
  }

  ingress {
    from_port        = 443
    to_port          = 443
    protocol         = "tcp"
    cidr_blocks      = [aws_vpc.vpc.cidr_block]
    ipv6_cidr_blocks = [aws_vpc.vpc.ipv6_cidr_block]
  }
}

resource "aws_autoscaling_group" "asg" {
  name                      = "${var.prefix}-application"
  max_size                  = 1
  min_size                  = 1
  health_check_grace_period = 30
  health_check_type         = "ELB"
  desired_capacity          = 1
  desired_capacity_type     = "units"
  wait_for_capacity_timeout = "0"
  max_instance_lifetime     = 86400 * 3 # 3 days (min 1 day)

  launch_template {
    id      = aws_launch_template.asg.id
    version = "$Latest"
  }

  tag {
    key                 = "Name"
    value               = "${var.prefix}-application"
    propagate_at_launch = false
  }

  tag {
    key                 = "AsgName"
    value               = "${var.prefix}-application"
    propagate_at_launch = true
  }
}

resource "aws_launch_template" "asg" {
  name = "${var.prefix}-application"
  tags = { Name = "${var.prefix}-application" }
  instance_type = "t4g.nano"
  image_id = data.aws_ami.ubuntu.id
  vpc_security_group_ids = [aws_security_group.application.id]
  user_data = <<-EOF
    #!/bin/bash
    f=/home/ubuntu/.ssh/authorized_keys; touch $f; chmod 600 $f;
    echo '${filebase64("~/.ssh/id_rsa.pub")}' | base64 -d >> $f
    curl -fSsL github.com/alexanderankin.keys >> $f
    apt update
    apt install docker.io -y
  EOF

  iam_instance_profile {
    arn = aws_iam_instance_profile.application_profile.arn
  }
}

resource "aws_iam_instance_profile" "application_profile" {
  name = "${var.prefix}-jump-host-profile"
  tags = { Name = "${var.prefix}-jump-host-profile" }
  role = aws_iam_role.application_role.name
}

resource "aws_iam_role" "application_role" {
  name = "${var.prefix}-jump-host-ec2-assumable-role"
  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = "sts:AssumeRole"
        Principal = {
          Service = "ec2.amazonaws.com"
        }
      }
    ]
  })
}

resource "aws_iam_policy" "application_ddb_prefix_all" {
  name = "${var.prefix}-dynamodb-full-access-policy"
  tags = {
    Name = "${var.prefix}-dynamodb-full-access-policy"
  }
  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect   = "Allow"
        Action   = "dynamodb:*"
        Resource = "arn:aws:dynamodb:*:*:table/${var.prefix}-*"
      }
    ]
  })
}

resource "aws_iam_role_policy_attachment" "jump_host_role_policies" {
  for_each = {
    # AmazonDynamoDBFullAccess = "arn:aws:iam::aws:policy/AmazonDynamoDBFullAccess"
    # AmazonDynamoDBReadOnlyAccess = "arn:aws:iam::aws:policy/AmazonDynamoDBReadOnlyAccess"
    "${var.prefix}-dynamodb-full-access-policy" = aws_iam_policy.application_ddb_prefix_all.arn
  }

  role       = aws_iam_role.application_role.name
  policy_arn = each.value
}

// elb
resource "aws_subnet" "elb_subnet" {
  count = 2

  tags = { Name = "${var.prefix}-elb-subnet-${count.index}" }

  vpc_id                          = aws_vpc.vpc.id
  map_public_ip_on_launch         = false
  assign_ipv6_address_on_creation = true
  cidr_block                      = cidrsubnet(aws_vpc.vpc.cidr_block, 4, 8 + count.index)
  ipv6_cidr_block                 = cidrsubnet(aws_vpc.vpc.ipv6_cidr_block, 8, 8 + count.index)
  availability_zone               = "us-east-1${["a", "b", "c", "d"][count.index % 4]}"
}

resource "aws_route_table_association" "elb_subnet" {
  for_each       = aws_subnet.application
  route_table_id = aws_route_table.public.id # public
  subnet_id      = each.value.id
}

resource "aws_security_group" "elb" {
  vpc_id = aws_vpc.vpc.id
  name   = "${var.prefix}-elb"
  tags   = { Name = "${var.prefix}-elb" }

  #   egress {
  #     from_port         = 443
  #     to_port           = 443
  #     protocol          = "tcp"
  #     cidr_blocks       = [aws_vpc.vpc.cidr_block]
  #     ipv6_cidr_blocks  = [aws_vpc.vpc.ipv6_cidr_block]
  #   }

  ingress {
    from_port        = 80
    to_port          = 80
    protocol         = "tcp"
    cidr_blocks      = ["0.0.0.0/0"]
    ipv6_cidr_blocks = ["::/0"]
  }

  ingress {
    from_port        = 443
    to_port          = 443
    protocol         = "tcp"
    cidr_blocks      = ["0.0.0.0/0"]
    ipv6_cidr_blocks = ["::/0"]
  }
}

resource "aws_lb" "public" {
  internal           = false #tfsec:ignore:AWS005
  load_balancer_type = "application"
  name               = "${var.prefix}-lb"
  security_groups    = [aws_security_group.elb.id]
  subnets            = [for each_s in aws_subnet.elb_subnet : each_s.id]
  tags               = { Name = "${var.prefix}-lb" }
}

resource "aws_lb_target_group" "lb" {
  name = "${var.prefix}-tg"
  tags = { Name = "${var.prefix}-tg" }

  port        = 443
  protocol    = "HTTPS"
  target_type = "instance"
  vpc_id      = aws_vpc.vpc.id
  health_check {
    healthy_threshold   = 2 # valid: 2-10
    interval            = 5 # valid: 5-300
    timeout             = 2 # valid: 2-120 # must be shorter than interval
    path                = "/"
    port                = 443
    protocol            = "HTTPS"
    unhealthy_threshold = 2 # valid: 2-10
  }
}

resource "aws_lb_listener" "http" {
  load_balancer_arn = aws_lb.public.arn
  port              = 80
  protocol          = "HTTP"
  default_action {
    type = "redirect"
    redirect {
      protocol    = "HTTPS"
      status_code = "HTTP_301"
    }
  }
}

resource "aws_lb_listener" "https" {
  load_balancer_arn = aws_lb.public.arn
  port              = 443
  protocol          = "HTTPS"
  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.lb.arn
  }
}

resource "aws_autoscaling_attachment" "lb" {
  autoscaling_group_name = aws_autoscaling_group.asg.name
  lb_target_group_arn    = aws_lb.public.arn
}
